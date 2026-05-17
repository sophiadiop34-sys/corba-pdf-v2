package com.pdfcorba.service;

import com.pdfcorba.corba.CORBAServer;
import com.pdfcorba.corba.PDFServiceImpl;
import com.pdfcorba.corba.PDFServiceModule;
import com.pdfcorba.model.ApiModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PdfService - Couche Service Spring Boot
 * 
 * Fait le pont entre les contrôleurs REST et le servant CORBA.
 * Gère le stockage temporaire des fichiers résultants.
 * 
 * Architecture : Controller → PdfService → CORBAServer → PDFServiceImpl → PDFBox
 */
@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    @Autowired
    private CORBAServer corbaServer;

    // Stockage temporaire des résultats (en mémoire)
    // Clé: UUID unique, Valeur: [nomFichier, données]
    private final ConcurrentHashMap<String, byte[]>  resultStore    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String>  fileNameStore  = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────
    // MÉTHODES INTERNES
    // ─────────────────────────────────────────────────────────────────

    /**
     * Obtient le servant CORBA (via le serveur CORBA)
     */
    private PDFServiceImpl getServant() {
        return corbaServer.getPdfServant();
    }

    /**
     * Stocke un résultat et retourne son identifiant de téléchargement
     */
    private String storeResult(byte[] data, String fileName) {
        String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        resultStore.put(id, data);
        fileNameStore.put(id, fileName);
        log.info("Résultat stocké → ID: {} | Fichier: {} | Taille: {} bytes", id, fileName, data.length);
        return id;
    }

    /**
     * Récupère un résultat par son identifiant
     */
    public byte[] getResult(String id) {
        return resultStore.get(id);
    }

    /**
     * Récupère le nom de fichier par l'identifiant
     */
    public String getResultFileName(String id) {
        return fileNameStore.getOrDefault(id, "result.pdf");
    }

    /**
     * Formate la taille en octets de façon lisible
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return new DecimalFormat("#.#").format(bytes / 1024.0) + " KB";
        return new DecimalFormat("#.##").format(bytes / (1024.0 * 1024)) + " MB";
    }

    // ─────────────────────────────────────────────────────────────────
    // GESTION PDF
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fusion de PDFs
     */
    public ApiModels.FileResponse mergePDFs(MultipartFile[] files) throws Exception {
        log.info("[SERVICE] mergePDFs - {} fichiers", files.length);

        if (files.length < 2) {
            throw new IllegalArgumentException("Au moins 2 fichiers PDF requis.");
        }

        byte[][] pdfData = new byte[files.length][];
        for (int i = 0; i < files.length; i++) {
            pdfData[i] = files[i].getBytes();
        }

        byte[] result = getServant().mergePDFs(pdfData);
        String id     = storeResult(result, "fusion_result.pdf");

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id,
            "fusion_result.pdf",
            result.length,
            "Fusion réussie de " + files.length + " PDFs → " + formatSize(result.length)
        );
    }

    /**
     * Découpage d'un PDF en pages
     */
    public List<ApiModels.FileResponse> splitPDF(MultipartFile file) throws Exception {
        log.info("[SERVICE] splitPDF - {}", file.getOriginalFilename());

        byte[][] pages = getServant().splitPDF(file.getBytes());
        List<ApiModels.FileResponse> responses = new ArrayList<>();

        for (int i = 0; i < pages.length; i++) {
            String id = storeResult(pages[i], "page_" + (i + 1) + ".pdf");
            responses.add(new ApiModels.FileResponse(
                "/api/pdf/download/" + id,
                "page_" + (i + 1) + ".pdf",
                pages[i].length,
                "Page " + (i + 1) + " → " + formatSize(pages[i].length)
            ));
        }

        return responses;
    }

    /**
     * Extraction de pages
     */
    public ApiModels.FileResponse extractPages(MultipartFile file, int start, int end)
            throws Exception {
        log.info("[SERVICE] extractPages {}-{}", start, end);

        byte[] result = getServant().extractPages(file.getBytes(), start, end);
        String id     = storeResult(result, "pages_" + start + "_" + end + ".pdf");

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id,
            "pages_" + start + "_" + end + ".pdf",
            result.length,
            "Pages " + start + " à " + end + " extraites → " + formatSize(result.length)
        );
    }

    /**
     * Suppression de pages
     */
    public ApiModels.FileResponse deletePages(MultipartFile file, String pagesStr)
            throws Exception {
        int[] pages = parsePageNumbers(pagesStr);
        log.info("[SERVICE] deletePages - pages: {}", Arrays.toString(pages));

        byte[] result = getServant().deletePages(file.getBytes(), pages);
        String id     = storeResult(result, "deleted_pages_result.pdf");

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id,
            "deleted_pages_result.pdf",
            result.length,
            pages.length + " page(s) supprimée(s) → " + formatSize(result.length)
        );
    }

    /**
     * Réorganisation des pages
     */
    public ApiModels.FileResponse reorderPages(MultipartFile file, String orderStr)
            throws Exception {
        int[] order = parsePageNumbers(orderStr);
        log.info("[SERVICE] reorderPages - ordre: {}", Arrays.toString(order));

        byte[] result = getServant().reorderPages(file.getBytes(), order);
        String id     = storeResult(result, "reordered_result.pdf");

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id,
            "reordered_result.pdf",
            result.length,
            "Pages réorganisées → " + formatSize(result.length)
        );
    }

    /**
     * Rotation des pages
     */
    public ApiModels.FileResponse rotatePages(MultipartFile file, String pagesStr, int angle)
            throws Exception {
        int[] pages = (pagesStr == null || pagesStr.trim().isEmpty())
            ? new int[0]
            : parsePageNumbers(pagesStr);

        log.info("[SERVICE] rotatePages - angle: {}°", angle);

        byte[] result = getServant().rotatePages(file.getBytes(), pages, angle);
        String id     = storeResult(result, "rotated_" + angle + "deg.pdf");

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id,
            "rotated_" + angle + "deg.pdf",
            result.length,
            "Rotation de " + angle + "° appliquée → " + formatSize(result.length)
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // ANALYSE PDF
    // ─────────────────────────────────────────────────────────────────

    /**
     * Extraction de texte
     */
    public String extractText(MultipartFile file) throws Exception {
        log.info("[SERVICE] extractText - {}", file.getOriginalFilename());
        return getServant().extractText(file.getBytes());
    }

    /**
     * Recherche de texte
     */
    public List<ApiModels.SearchResultResponse> searchText(MultipartFile file, String term)
            throws Exception {
        log.info("[SERVICE] searchText - '{}'", term);

        PDFServiceModule.SearchResult[] raw = getServant().searchText(file.getBytes(), term);
        List<ApiModels.SearchResultResponse> results = new ArrayList<>();

        for (PDFServiceModule.SearchResult r : raw) {
            results.add(new ApiModels.SearchResultResponse(
                r.pageNumber, r.context, r.occurrences
            ));
        }

        return results;
    }

    /**
     * Informations du PDF
     */
    public ApiModels.PdfInfoResponse getPDFInfo(MultipartFile file) throws Exception {
        log.info("[SERVICE] getPDFInfo - {}", file.getOriginalFilename());

        PDFServiceModule.PDFInfo info = getServant().getPDFInfo(
            file.getBytes(),
            file.getOriginalFilename()
        );

        ApiModels.PdfInfoResponse response = new ApiModels.PdfInfoResponse();
        response.setFileName(info.fileName);
        response.setPageCount(info.pageCount);
        response.setFileSizeBytes(info.fileSizeBytes);
        response.setFileSize(formatSize(info.fileSizeBytes));
        response.setWordCount(info.wordCount);
        response.setAuthor(info.author);
        response.setTitle(info.title);
        response.setCreationDate(info.creationDate);

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // SÉCURITÉ & TRANSFORMATION
    // ─────────────────────────────────────────────────────────────────

    /**
     * Protection par mot de passe
     */
    public ApiModels.FileResponse addPassword(MultipartFile file,
                                               String ownerPass, String userPass)
            throws Exception {
        log.info("[SERVICE] addPassword");

        byte[] result = getServant().addPassword(file.getBytes(), ownerPass, userPass);
        String id     = storeResult(result, "protected_" + file.getOriginalFilename());

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id,
            "protected_" + file.getOriginalFilename(),
            result.length,
            "PDF protégé par mot de passe → " + formatSize(result.length)
        );
    }

    /**
     * Ajout de filigrane
     */
    public ApiModels.FileResponse addWatermark(MultipartFile file,
                                                String text, float opacity)
            throws Exception {
        log.info("[SERVICE] addWatermark '{}'", text);

        byte[] result = getServant().addWatermark(file.getBytes(), text, opacity);
        String id     = storeResult(result, "watermarked_" + file.getOriginalFilename());

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id,
            "watermarked_" + file.getOriginalFilename(),
            result.length,
            "Filigrane '" + text + "' ajouté → " + formatSize(result.length)
        );
    }

    /**
     * Compression PDF
     */
    public ApiModels.FileResponse compressPDF(MultipartFile file) throws Exception {
        log.info("[SERVICE] compressPDF - original: {} bytes", file.getSize());

        byte[] result  = getServant().compressPDF(file.getBytes());
        String id      = storeResult(result, "compressed_" + file.getOriginalFilename());
        long   saved   = file.getSize() - result.length;
        String savings = saved > 0 ? " (économie: " + formatSize(saved) + ")" : "";

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id,
            "compressed_" + file.getOriginalFilename(),
            result.length,
            formatSize(file.getSize()) + " → " + formatSize(result.length) + savings
        );
    }

    /**
     * Conversion en image
     */
    public ApiModels.FileResponse convertToImage(MultipartFile file, int page, int dpi)
            throws Exception {
        log.info("[SERVICE] convertToImage - page: {}, dpi: {}", page, dpi);

        byte[] result  = getServant().convertToImage(file.getBytes(), page, dpi);
        String id      = storeResult(result, "page_" + page + "_" + dpi + "dpi.png");

        // Stocker comme PNG
        fileNameStore.put(id, "page_" + page + ".png");

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id + "?type=image",
            "page_" + page + ".png",
            result.length,
            "Page " + page + " convertie en PNG à " + dpi + " DPI → " + formatSize(result.length)
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // CRÉATION PDF
    // ─────────────────────────────────────────────────────────────────

    /**
     * Création d'un nouveau PDF
     */
    public ApiModels.FileResponse createPDF(String title, String content, String author)
            throws Exception {
        log.info("[SERVICE] createPDF '{}'", title);

        byte[] result = getServant().createPDF(title, content, author);
        String fileName = title.replaceAll("[^a-zA-Z0-9_-]", "_") + ".pdf";
        String id     = storeResult(result, fileName);

        return new ApiModels.FileResponse(
            "/api/pdf/download/" + id,
            fileName,
            result.length,
            "PDF '" + title + "' créé avec succès → " + formatSize(result.length)
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parse une chaîne de numéros de pages "1,3,5" → [1, 3, 5]
     */
    private int[] parsePageNumbers(String pagesStr) {
        if (pagesStr == null || pagesStr.trim().isEmpty()) {
            return new int[0];
        }
        String[] parts = pagesStr.split("[,;\\s]+");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Numéro de page invalide: '" + parts[i] + "'"
                );
            }
        }
        return result;
    }

    /**
     * Retourne le statut du serveur CORBA
     */
    public ApiModels.ServerStatus getServerStatus() {
        ApiModels.ServerStatus status = new ApiModels.ServerStatus();
        status.setCorbaRunning(corbaServer.isRunning());
        status.setCorbaInfo(corbaServer.getServerInfo());
        status.setSpringVersion("Spring Boot 2.7.x");
        status.setJavaVersion(System.getProperty("java.version"));
        status.setServerTime(
            new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                .format(new java.util.Date())
        );
        return status;
    }
}
