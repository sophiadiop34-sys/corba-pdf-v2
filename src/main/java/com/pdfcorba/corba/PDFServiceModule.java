package com.pdfcorba.corba;

/**
 * PDFServiceModule - Classes générées depuis l'IDL CORBA
 * 
 * Dans un vrai projet CORBA, ces classes seraient générées via :
 *   idlj -fall PDFService.idl
 * 
 * Ici nous les implémentons manuellement pour la compatibilité Docker.
 * L'architecture CORBA est respectée via le pattern Servant/ORB.
 */
public class PDFServiceModule {

    /**
     * Structure PDFInfo - informations du document
     */
    public static class PDFInfo {
        public String fileName;
        public int    pageCount;
        public long   fileSizeBytes;
        public int    wordCount;
        public String author;
        public String title;
        public String creationDate;

        public PDFInfo() {}

        public PDFInfo(String fileName, int pageCount, long fileSizeBytes,
                       int wordCount, String author, String title, String creationDate) {
            this.fileName      = fileName;
            this.pageCount     = pageCount;
            this.fileSizeBytes = fileSizeBytes;
            this.wordCount     = wordCount;
            this.author        = author;
            this.title         = title;
            this.creationDate  = creationDate;
        }
    }

    /**
     * Structure SearchResult - résultat de recherche textuelle
     */
    public static class SearchResult {
        public int    pageNumber;
        public String context;
        public int    occurrences;

        public SearchResult() {}

        public SearchResult(int pageNumber, String context, int occurrences) {
            this.pageNumber  = pageNumber;
            this.context     = context;
            this.occurrences = occurrences;
        }
    }

    /**
     * Exception CORBA personnalisée
     */
    public static class PDFException extends Exception {
        public String message;

        public PDFException(String message) {
            super(message);
            this.message = message;
        }
    }

    /**
     * Interface CORBA PDFService - correspondance exacte avec l'IDL
     */
    public interface PDFService {

        // ─── GESTION PDF ──────────────────────────────────────────
        byte[]   mergePDFs(byte[][] pdfFiles)     throws PDFException;
        byte[][] splitPDF(byte[] pdfData)          throws PDFException;
        byte[]   extractPages(byte[] pdfData, int startPage, int endPage) throws PDFException;
        byte[]   deletePages(byte[] pdfData, int[] pageNumbers)           throws PDFException;
        byte[]   reorderPages(byte[] pdfData, int[] newOrder)             throws PDFException;
        byte[]   rotatePages(byte[] pdfData, int[] pageNumbers, int angle) throws PDFException;

        // ─── ANALYSE PDF ──────────────────────────────────────────
        String         extractText(byte[] pdfData)                          throws PDFException;
        SearchResult[] searchText(byte[] pdfData, String searchTerm)        throws PDFException;
        PDFInfo        getPDFInfo(byte[] pdfData, String fileName)           throws PDFException;

        // ─── SÉCURITÉ & TRANSFORMATION ───────────────────────────
        byte[] addPassword(byte[] pdfData, String ownerPassword, String userPassword) throws PDFException;
        byte[] addWatermark(byte[] pdfData, String watermarkText, float opacity)      throws PDFException;
        byte[] compressPDF(byte[] pdfData)                                             throws PDFException;
        byte[] convertToImage(byte[] pdfData, int pageNumber, int dpi)                throws PDFException;

        // ─── CRÉATION ────────────────────────────────────────────
        byte[] createPDF(String title, String content, String author) throws PDFException;

        // ─── UTILITAIRES ─────────────────────────────────────────
        String ping();
    }
}
