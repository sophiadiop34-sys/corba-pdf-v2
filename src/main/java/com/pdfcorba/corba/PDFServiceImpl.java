package com.pdfcorba.corba;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDFServiceImpl - Servant CORBA
 *
 * Implémentation de toutes les opérations PDF définies dans l'IDL.
 * Utilise Apache PDFBox pour la manipulation réelle des fichiers.
 *
 * Hérite de Servant (PortableServer) pour s'intégrer dans le POA CORBA.
 */
public class PDFServiceImpl extends Servant
        implements PDFServiceModule.PDFService {

    private static final Logger log = LoggerFactory.getLogger(PDFServiceImpl.class);

    // ─────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────

    @Override
    public String ping() {
        log.info("[CORBA] ping() appelé");
        return "CORBA PDFService opérationnel ! Timestamp: " +
               new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    // ─────────────────────────────────────────────────────────────────
    // GESTION PDF
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fusionne plusieurs PDFs en un seul document
     */
    @Override
    public byte[] mergePDFs(byte[][] pdfFiles) throws PDFServiceModule.PDFException {
        log.info("[CORBA] mergePDFs() - Fusion de {} fichiers PDF", pdfFiles.length);

        if (pdfFiles == null || pdfFiles.length < 2) {
            throw new PDFServiceModule.PDFException("Au moins 2 fichiers PDF requis pour la fusion");
        }

        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            merger.setDestinationStream(outputStream);

            for (int i = 0; i < pdfFiles.length; i++) {
                merger.addSource(new ByteArrayInputStream(pdfFiles[i]));
                log.debug("  → PDF #{} ajouté ({} bytes)", i + 1, pdfFiles[i].length);
            }

            merger.mergeDocuments(null);
            byte[] result = outputStream.toByteArray();
            log.info("[CORBA] mergePDFs() - Fusion réussie: {} bytes", result.length);
            return result;

        } catch (IOException e) {
            log.error("[CORBA] Erreur mergePDFs: {}", e.getMessage());
            throw new PDFServiceModule.PDFException("Erreur lors de la fusion: " + e.getMessage());
        }
    }

    /**
     * Découpe un PDF en pages individuelles
     */
    @Override
    public byte[][] splitPDF(byte[] pdfData) throws PDFServiceModule.PDFException {
        log.info("[CORBA] splitPDF() - Découpage du PDF ({} bytes)", pdfData.length);

        try (PDDocument document = PDDocument.load(pdfData)) {
            Splitter splitter = new Splitter();
            List<PDDocument> pages = splitter.split(document);
            byte[][] result = new byte[pages.size()][];

            for (int i = 0; i < pages.size(); i++) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                pages.get(i).save(baos);
                pages.get(i).close();
                result[i] = baos.toByteArray();
            }

            log.info("[CORBA] splitPDF() - {} pages extraites", result.length);
            return result;

        } catch (IOException e) {
            log.error("[CORBA] Erreur splitPDF: {}", e.getMessage());
            throw new PDFServiceModule.PDFException("Erreur lors du découpage: " + e.getMessage());
        }
    }

    /**
     * Extrait une plage de pages
     */
    @Override
    public byte[] extractPages(byte[] pdfData, int startPage, int endPage)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] extractPages() - Pages {} à {}", startPage, endPage);

        try (PDDocument source = PDDocument.load(pdfData)) {
            int totalPages = source.getNumberOfPages();

            if (startPage < 1 || endPage > totalPages || startPage > endPage) {
                throw new PDFServiceModule.PDFException(
                    "Pages invalides. Le PDF a " + totalPages + " pages. Demandé: " + startPage + "-" + endPage
                );
            }

            PDDocument result = new PDDocument();
            for (int i = startPage - 1; i < endPage; i++) {
                result.addPage(source.getPage(i));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.save(baos);
            result.close();

            log.info("[CORBA] extractPages() - {} pages extraites", endPage - startPage + 1);
            return baos.toByteArray();

        } catch (PDFServiceModule.PDFException e) {
            throw e;
        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur extraction: " + e.getMessage());
        }
    }

    /**
     * Supprime des pages spécifiques
     */
    @Override
    public byte[] deletePages(byte[] pdfData, int[] pageNumbers)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] deletePages() - Suppression de {} pages", pageNumbers.length);

        try (PDDocument source = PDDocument.load(pdfData)) {
            int totalPages = source.getNumberOfPages();

            Set<Integer> toDelete = new HashSet<>();
            for (int p : pageNumbers) {
                if (p < 1 || p > totalPages) {
                    throw new PDFServiceModule.PDFException(
                        "Page " + p + " invalide. Le PDF a " + totalPages + " pages."
                    );
                }
                toDelete.add(p - 1);
            }

            PDDocument result = new PDDocument();
            for (int i = 0; i < totalPages; i++) {
                if (!toDelete.contains(i)) {
                    result.addPage(source.getPage(i));
                }
            }

            if (result.getNumberOfPages() == 0) {
                result.close();
                throw new PDFServiceModule.PDFException(
                    "Impossible de supprimer toutes les pages du PDF."
                );
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.save(baos);
            result.close();

            log.info("[CORBA] deletePages() - {} pages restantes", result.getNumberOfPages());
            return baos.toByteArray();

        } catch (PDFServiceModule.PDFException e) {
            throw e;
        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur suppression: " + e.getMessage());
        }
    }

    /**
     * Réorganise les pages dans un nouvel ordre
     */
    @Override
    public byte[] reorderPages(byte[] pdfData, int[] newOrder)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] reorderPages() - Réorganisation de {} pages", newOrder.length);

        try (PDDocument source = PDDocument.load(pdfData)) {
            int totalPages = source.getNumberOfPages();

            if (newOrder.length != totalPages) {
                throw new PDFServiceModule.PDFException(
                    "L'ordre doit contenir exactement " + totalPages + " numéros de pages."
                );
            }

            PDDocument result = new PDDocument();
            for (int pageNum : newOrder) {
                if (pageNum < 1 || pageNum > totalPages) {
                    result.close();
                    throw new PDFServiceModule.PDFException(
                        "Numéro de page invalide: " + pageNum
                    );
                }
                result.addPage(source.getPage(pageNum - 1));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.save(baos);
            result.close();

            return baos.toByteArray();

        } catch (PDFServiceModule.PDFException e) {
            throw e;
        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur réorganisation: " + e.getMessage());
        }
    }

    /**
     * Rotation des pages (90, 180 ou 270 degrés)
     */
    @Override
    public byte[] rotatePages(byte[] pdfData, int[] pageNumbers, int angle)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] rotatePages() - Rotation de {}° sur {} pages", angle, pageNumbers.length);

        if (angle != 90 && angle != 180 && angle != 270) {
            throw new PDFServiceModule.PDFException(
                "Angle invalide: " + angle + ". Valeurs acceptées: 90, 180, 270"
            );
        }

        try (PDDocument document = PDDocument.load(pdfData)) {
            int totalPages = document.getNumberOfPages();

            int[] pages = (pageNumbers == null || pageNumbers.length == 0)
                ? generateAllPages(totalPages)
                : pageNumbers;

            for (int pageNum : pages) {
                if (pageNum < 1 || pageNum > totalPages) {
                    throw new PDFServiceModule.PDFException(
                        "Page " + pageNum + " invalide."
                    );
                }
                PDPage page = document.getPage(pageNum - 1);
                int currentRotation = page.getRotation();
                page.setRotation((currentRotation + angle) % 360);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (PDFServiceModule.PDFException e) {
            throw e;
        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur rotation: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ANALYSE PDF
    // ─────────────────────────────────────────────────────────────────

    /**
     * Extrait tout le texte d'un PDF
     */
    @Override
    public String extractText(byte[] pdfData) throws PDFServiceModule.PDFException {
        log.info("[CORBA] extractText() - Extraction du texte");

        try (PDDocument document = PDDocument.load(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            log.info("[CORBA] extractText() - {} caractères extraits", text.length());
            return text;

        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur extraction texte: " + e.getMessage());
        }
    }

    /**
     * Recherche un terme dans le PDF avec contexte
     */
    @Override
    public PDFServiceModule.SearchResult[] searchText(byte[] pdfData, String searchTerm)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] searchText() - Recherche de '{}'", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new PDFServiceModule.PDFException("Terme de recherche vide.");
        }

        try (PDDocument document = PDDocument.load(pdfData)) {
            List<PDFServiceModule.SearchResult> results = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            Pattern pattern = Pattern.compile(
                Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE
            );

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(document);

                Matcher matcher = pattern.matcher(pageText);
                int count = 0;
                int firstPos = -1;

                while (matcher.find()) {
                    count++;
                    if (firstPos == -1) firstPos = matcher.start();
                }

                if (count > 0) {
                    int start = Math.max(0, firstPos - 50);
                    int end   = Math.min(pageText.length(), firstPos + searchTerm.length() + 50);
                    String context = "..." + pageText.substring(start, end).trim() + "...";
                    context = context.replaceAll("\\s+", " ");
                    results.add(new PDFServiceModule.SearchResult(pageNum, context, count));
                }
            }

            log.info("[CORBA] searchText() - {} pages avec des résultats", results.size());
            return results.toArray(new PDFServiceModule.SearchResult[0]);

        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur recherche: " + e.getMessage());
        }
    }

    /**
     * Retourne les métadonnées et statistiques du PDF
     */
    @Override
    public PDFServiceModule.PDFInfo getPDFInfo(byte[] pdfData, String fileName)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] getPDFInfo() - Analyse de '{}'", fileName);

        try (PDDocument document = PDDocument.load(pdfData)) {
            PDDocumentInformation info = document.getDocumentInformation();

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            int wordCount = text.trim().isEmpty() ? 0
                : text.trim().split("\\s+").length;

            String creationDate = "N/A";
            if (info.getCreationDate() != null) {
                creationDate = new SimpleDateFormat("dd/MM/yyyy HH:mm")
                    .format(info.getCreationDate().getTime());
            }

            PDFServiceModule.PDFInfo pdfInfo = new PDFServiceModule.PDFInfo(
                fileName,
                document.getNumberOfPages(),
                pdfData.length,
                wordCount,
                info.getAuthor()   != null ? info.getAuthor()   : "Inconnu",
                info.getTitle()    != null ? info.getTitle()     : "Sans titre",
                creationDate
            );

            log.info("[CORBA] getPDFInfo() - {} pages, {} mots",
                     pdfInfo.pageCount, pdfInfo.wordCount);
            return pdfInfo;

        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur lecture info: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SÉCURITÉ & TRANSFORMATION
    // ─────────────────────────────────────────────────────────────────

    /**
     * Protège un PDF par mot de passe
     */
    @Override
    public byte[] addPassword(byte[] pdfData, String ownerPassword, String userPassword)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] addPassword() - Protection par mot de passe");

        try (PDDocument document = PDDocument.load(pdfData)) {
            AccessPermission ap = new AccessPermission();
            ap.setCanPrint(true);
            ap.setCanExtractContent(false);
            ap.setCanModify(false);

            StandardProtectionPolicy spp =
                new StandardProtectionPolicy(ownerPassword, userPassword, ap);
            spp.setEncryptionKeyLength(128);

            document.protect(spp);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);

            log.info("[CORBA] addPassword() - PDF protégé avec succès");
            return baos.toByteArray();

        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur protection: " + e.getMessage());
        }
    }

    /**
     * Ajoute un filigrane (watermark) sur toutes les pages
     */
    @Override
    public byte[] addWatermark(byte[] pdfData, String watermarkText, float opacity)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] addWatermark() - Ajout du filigrane: '{}'", watermarkText);

        if (watermarkText == null || watermarkText.trim().isEmpty()) {
            throw new PDFServiceModule.PDFException("Texte du filigrane vide.");
        }
        if (opacity < 0.0f || opacity > 1.0f) {
            opacity = 0.3f;
        }

        try (PDDocument document = PDDocument.load(pdfData)) {
            int totalPages = document.getNumberOfPages();

            for (int i = 0; i < totalPages; i++) {
                PDPage page = document.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                float pageWidth  = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();

                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page,
                        PDPageContentStream.AppendMode.APPEND,
                        true, true)) {

                    PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                    gs.setNonStrokingAlphaConstant(opacity);
                    gs.setAlphaSourceFlag(true);
                    cs.setGraphicsStateParameters(gs);

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 52);
                    cs.setNonStrokingColor(Color.LIGHT_GRAY);

                    AffineTransform at = new AffineTransform(
                        pageWidth / 9, 0,
                        0, pageHeight / 9,
                        pageWidth / 2 - 100,
                        pageHeight / 2
                    );
                    at.rotate(Math.toRadians(45));
                    cs.setTextMatrix(
                        (float) at.getScaleX(),  (float) at.getShearY(),
                        (float) at.getShearX(),  (float) at.getScaleY(),
                        (float) at.getTranslateX(), (float) at.getTranslateY()
                    );

                    cs.showText(watermarkText.toUpperCase());
                    cs.endText();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);

            log.info("[CORBA] addWatermark() - Filigrane ajouté sur {} pages", totalPages);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur filigrane: " + e.getMessage());
        }
    }

    /**
     * Compresse un PDF (réduction de la taille)
     */
    @Override
    public byte[] compressPDF(byte[] pdfData) throws PDFServiceModule.PDFException {
        log.info("[CORBA] compressPDF() - Compression ({} bytes)", pdfData.length);

        try (PDDocument document = PDDocument.load(pdfData)) {
            document.setVersion(1.5f);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            byte[] compressed = baos.toByteArray();

            log.info("[CORBA] compressPDF() - {} bytes → {} bytes",
                     pdfData.length, compressed.length);
            return compressed;

        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur compression: " + e.getMessage());
        }
    }

    /**
     * Convertit une page PDF en image PNG
     */
    @Override
    public byte[] convertToImage(byte[] pdfData, int pageNumber, int dpi)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] convertToImage() - Page {} à {} DPI", pageNumber, dpi);

        int safeDpi = (dpi <= 0 || dpi > 600) ? 150 : dpi;

        try (PDDocument document = PDDocument.load(pdfData)) {
            int totalPages = document.getNumberOfPages();

            if (pageNumber < 1 || pageNumber > totalPages) {
                throw new PDFServiceModule.PDFException(
                    "Page " + pageNumber + " invalide. Le PDF a " + totalPages + " pages."
                );
            }

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, safeDpi);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);

            log.info("[CORBA] convertToImage() - Image {}x{} générée ({} bytes)",
                     image.getWidth(), image.getHeight(), baos.size());
            return baos.toByteArray();

        } catch (PDFServiceModule.PDFException e) {
            throw e;
        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur conversion image: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // CRÉATION PDF
    // ─────────────────────────────────────────────────────────────────

    /**
     * Crée un nouveau PDF à partir d'un contenu texte
     */
    @Override
    public byte[] createPDF(String title, String content, String author)
            throws PDFServiceModule.PDFException {
        log.info("[CORBA] createPDF() - Création: '{}'", title);

        try (PDDocument document = new PDDocument()) {
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle(title);
            info.setAuthor(author != null ? author : "CORBA PDF Service");
            info.setCreator("CORBA PDF Service v1.0");
            info.setProducer("Apache PDFBox 2.x");
            info.setCreationDate(Calendar.getInstance());

            String[] lines = (content != null ? content : "").split("\n");
            float margin     = 50;
            float yStart     = 750;
            float lineHeight = 16;
            float yPos       = yStart;

            PDPage currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);

            PDPageContentStream cs = new PDPageContentStream(document, currentPage);
            boolean firstPage = true;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (yPos < margin + 40) {
                    cs.endText();
                    cs.close();
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    cs = new PDPageContentStream(document, currentPage);
                    yPos = yStart;
                    firstPage = false;

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                    cs.setNonStrokingColor(Color.GRAY);
                    cs.newLineAtOffset(margin, 800);
                    cs.showText(title + " — " + author);
                    cs.endText();
                }

                if (firstPage && i == 0) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
                    cs.setNonStrokingColor(new Color(30, 80, 160));
                    cs.newLineAtOffset(margin, yPos);
                    cs.showText(title);
                    cs.endText();
                    yPos -= 35;

                    cs.setStrokingColor(new Color(30, 80, 160));
                    cs.setLineWidth(1.5f);
                    cs.moveTo(margin, yPos);
                    cs.lineTo(545, yPos);
                    cs.stroke();
                    yPos -= 15;

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 11);
                    cs.setNonStrokingColor(Color.DARK_GRAY);
                    cs.newLineAtOffset(margin, yPos);
                    cs.showText("Auteur : " + (author != null ? author : "N/A")
                        + "   |   " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                    cs.endText();
                    yPos -= 30;
                    firstPage = false;
                    continue;
                }

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.setNonStrokingColor(Color.BLACK);
                cs.newLineAtOffset(margin, yPos);

                String safeLine = line.replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", "");
                if (!safeLine.isEmpty()) {
                    cs.showText(safeLine);
                }
                cs.endText();
                yPos -= lineHeight;
            }

            cs.close();

            PDPage lastPage = document.getPage(document.getNumberOfPages() - 1);
            try (PDPageContentStream footer = new PDPageContentStream(
                    document, lastPage, PDPageContentStream.AppendMode.APPEND, true)) {
                footer.beginText();
                footer.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                footer.setNonStrokingColor(Color.GRAY);
                footer.newLineAtOffset(margin, 30);
                footer.showText("Généré par CORBA PDF Service | " +
                    new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
                footer.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);

            log.info("[CORBA] createPDF() - PDF créé: {} bytes, {} page(s)",
                     baos.size(), document.getNumberOfPages());
            return baos.toByteArray();

        } catch (IOException e) {
            throw new PDFServiceModule.PDFException("Erreur création PDF: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // MÉTHODES UTILITAIRES PRIVÉES
    // ─────────────────────────────────────────────────────────────────

    private int[] generateAllPages(int totalPages) {
        int[] pages = new int[totalPages];
        for (int i = 0; i < totalPages; i++) pages[i] = i + 1;
        return pages;
    }

    /**
     * Méthode CORBA requise pour le POA
     */
    @Override
    public String[] _all_interfaces(POA poa, byte[] objectId) {
        return new String[]{};
    }
}