
package com.pdfcorba.controller;

import com.pdfcorba.model.ApiModels;
import com.pdfcorba.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * PdfController - Contrôleur REST Spring Boot
 * 
 * Expose tous les endpoints API pour les opérations PDF.
 * Préfixe : /api/pdf
 */
@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "*")
public class PdfController {

    private static final Logger log = LoggerFactory.getLogger(PdfController.class);

    @Autowired
    private PdfService pdfService;

    // ─────────────────────────────────────────────────────────────────
    // STATUT
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<ApiModels.ApiResponse> getStatus() {
        try {
            ApiModels.ServerStatus status = pdfService.getServerStatus();
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("Serveur opérationnel", status));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiModels.ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GESTION PDF
    // ─────────────────────────────────────────────────────────────────

    /** POST /api/pdf/merge - Fusion de PDFs */
    @PostMapping("/merge")
    public ResponseEntity<ApiModels.ApiResponse> mergePDFs(
            @RequestParam("files") MultipartFile[] files) {
        try {
            validateFiles(files);
            ApiModels.FileResponse result = pdfService.mergePDFs(files);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("Fusion réussie !", result));
        } catch (Exception e) {
            log.error("Erreur merge: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/split - Découpage d'un PDF */
    @PostMapping("/split")
    public ResponseEntity<ApiModels.ApiResponse> splitPDF(
            @RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);
            List<ApiModels.FileResponse> results = pdfService.splitPDF(file);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok(
                "PDF découpé en " + results.size() + " pages", results));
        } catch (Exception e) {
            log.error("Erreur split: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/extract-pages - Extraction de pages */
    @PostMapping("/extract-pages")
    public ResponseEntity<ApiModels.ApiResponse> extractPages(
            @RequestParam("file") MultipartFile file,
            @RequestParam("start") int start,
            @RequestParam("end")   int end) {
        try {
            validateFile(file);
            ApiModels.FileResponse result = pdfService.extractPages(file, start, end);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("Pages extraites avec succès !", result));
        } catch (Exception e) {
            log.error("Erreur extractPages: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/delete-pages - Suppression de pages */
    @PostMapping("/delete-pages")
    public ResponseEntity<ApiModels.ApiResponse> deletePages(
            @RequestParam("file")  MultipartFile file,
            @RequestParam("pages") String pages) {
        try {
            validateFile(file);
            ApiModels.FileResponse result = pdfService.deletePages(file, pages);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("Pages supprimées avec succès !", result));
        } catch (Exception e) {
            log.error("Erreur deletePages: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/reorder - Réorganisation des pages */
    @PostMapping("/reorder")
    public ResponseEntity<ApiModels.ApiResponse> reorderPages(
            @RequestParam("file")  MultipartFile file,
            @RequestParam("order") String order) {
        try {
            validateFile(file);
            ApiModels.FileResponse result = pdfService.reorderPages(file, order);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("Pages réorganisées avec succès !", result));
        } catch (Exception e) {
            log.error("Erreur reorder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/rotate - Rotation des pages */
    @PostMapping("/rotate")
    public ResponseEntity<ApiModels.ApiResponse> rotatePages(
            @RequestParam("file")              MultipartFile file,
            @RequestParam(value="pages", required=false, defaultValue="") String pages,
            @RequestParam("angle")             int angle) {
        try {
            validateFile(file);
            ApiModels.FileResponse result = pdfService.rotatePages(file, pages, angle);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("Rotation appliquée avec succès !", result));
        } catch (Exception e) {
            log.error("Erreur rotate: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ANALYSE PDF
    // ─────────────────────────────────────────────────────────────────

    /** POST /api/pdf/extract-text - Extraction de texte */
    @PostMapping("/extract-text")
    public ResponseEntity<ApiModels.ApiResponse> extractText(
            @RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);
            String text = pdfService.extractText(file);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok(
                "Texte extrait (" + text.length() + " caractères)",
                text));
        } catch (Exception e) {
            log.error("Erreur extractText: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/search - Recherche de texte */
    @PostMapping("/search")
    public ResponseEntity<ApiModels.ApiResponse> searchText(
            @RequestParam("file") MultipartFile file,
            @RequestParam("term") String term) {
        try {
            validateFile(file);
            List<ApiModels.SearchResultResponse> results = pdfService.searchText(file, term);
            String msg = results.isEmpty()
                ? "Aucun résultat pour '" + term + "'"
                : results.size() + " page(s) contenant '" + term + "'";
            return ResponseEntity.ok(ApiModels.ApiResponse.ok(msg, results));
        } catch (Exception e) {
            log.error("Erreur search: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/info - Statistiques du PDF */
    @PostMapping("/info")
    public ResponseEntity<ApiModels.ApiResponse> getPDFInfo(
            @RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);
            ApiModels.PdfInfoResponse info = pdfService.getPDFInfo(file);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("Informations extraites", info));
        } catch (Exception e) {
            log.error("Erreur info: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SÉCURITÉ & TRANSFORMATION
    // ─────────────────────────────────────────────────────────────────

    /** POST /api/pdf/protect - Ajout de mot de passe */
    @PostMapping("/protect")
    public ResponseEntity<ApiModels.ApiResponse> addPassword(
            @RequestParam("file")           MultipartFile file,
            @RequestParam("ownerPassword")  String ownerPassword,
            @RequestParam("userPassword")   String userPassword) {
        try {
            validateFile(file);
            ApiModels.FileResponse result = pdfService.addPassword(file, ownerPassword, userPassword);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("PDF protégé avec succès !", result));
        } catch (Exception e) {
            log.error("Erreur protect: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/watermark - Ajout de filigrane */
    @PostMapping("/watermark")
    public ResponseEntity<ApiModels.ApiResponse> addWatermark(
            @RequestParam("file")                              MultipartFile file,
            @RequestParam("text")                              String text,
            @RequestParam(value="opacity", defaultValue="0.3") float opacity) {
        try {
            validateFile(file);
            ApiModels.FileResponse result = pdfService.addWatermark(file, text, opacity);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("Filigrane ajouté avec succès !", result));
        } catch (Exception e) {
            log.error("Erreur watermark: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/compress - Compression PDF */
    @PostMapping("/compress")
    public ResponseEntity<ApiModels.ApiResponse> compressPDF(
            @RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);
            ApiModels.FileResponse result = pdfService.compressPDF(file);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("PDF compressé avec succès !", result));
        } catch (Exception e) {
            log.error("Erreur compress: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /api/pdf/to-image - Conversion en image */
    @PostMapping("/to-image")
    public ResponseEntity<ApiModels.ApiResponse> convertToImage(
            @RequestParam("file")                           MultipartFile file,
            @RequestParam(value="page", defaultValue="1")   int page,
            @RequestParam(value="dpi",  defaultValue="150") int dpi) {
        try {
            validateFile(file);
            ApiModels.FileResponse result = pdfService.convertToImage(file, page, dpi);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("Page convertie en image !", result));
        } catch (Exception e) {
            log.error("Erreur to-image: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // CRÉATION
    // ─────────────────────────────────────────────────────────────────

    /** POST /api/pdf/create - Création d'un nouveau PDF */
    @PostMapping("/create")
    public ResponseEntity<ApiModels.ApiResponse> createPDF(
            @RequestParam("title")                            String title,
            @RequestParam("content")                          String content,
            @RequestParam(value="author", defaultValue="")    String author) {
        try {
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Le titre est obligatoire.");
            }
            ApiModels.FileResponse result = pdfService.createPDF(title, content, author);
            return ResponseEntity.ok(ApiModels.ApiResponse.ok("PDF créé avec succès !", result));
        } catch (Exception e) {
            log.error("Erreur create: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiModels.ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TÉLÉCHARGEMENT
    // ─────────────────────────────────────────────────────────────────

    /** GET /api/pdf/download/{id} - Téléchargement d'un résultat */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String id,
            @RequestParam(value="type", defaultValue="pdf") String type) {
        try {
            byte[] data     = pdfService.getResult(id);
            String fileName = pdfService.getResultFileName(id);

            if (data == null) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = "image".equals(type)
                ? MediaType.IMAGE_PNG
                : MediaType.APPLICATION_PDF;

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .contentLength(data.length)
                .body(data);

        } catch (Exception e) {
            log.error("Erreur download: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier fourni.");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Le fichier doit être un PDF (.pdf)");
        }
    }

    private void validateFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Aucun fichier fourni.");
        }
        for (MultipartFile f : files) {
            validateFile(f);
        }
    }
}
