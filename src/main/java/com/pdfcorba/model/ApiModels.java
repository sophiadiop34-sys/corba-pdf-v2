package com.pdfcorba.model;

import java.util.List;
import java.util.Map;

/**
 * Modèles de réponse pour l'API Spring Boot
 */
public class ApiModels {

    // ─────────────────────────────────────────────────────────────────
    // RÉPONSE GÉNÉRIQUE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Réponse standard de l'API
     */
    public static class ApiResponse {
        private boolean success;
        private String  message;
        private Object  data;

        public ApiResponse() {}

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data    = data;
        }

        public static ApiResponse ok(String message, Object data) {
            return new ApiResponse(true, message, data);
        }

        public static ApiResponse error(String message) {
            return new ApiResponse(false, message, null);
        }

        public boolean isSuccess()  { return success; }
        public String  getMessage() { return message; }
        public Object  getData()    { return data;    }

        public void setSuccess(boolean success) { this.success = success; }
        public void setMessage(String message)  { this.message = message; }
        public void setData(Object data)        { this.data = data;       }
    }

    // ─────────────────────────────────────────────────────────────────
    // INFOS PDF
    // ─────────────────────────────────────────────────────────────────

    /**
     * Informations/statistiques d'un PDF
     */
    public static class PdfInfoResponse {
        private String fileName;
        private int    pageCount;
        private String fileSize;
        private long   fileSizeBytes;
        private int    wordCount;
        private String author;
        private String title;
        private String creationDate;

        public PdfInfoResponse() {}

        public String getFileName()       { return fileName;       }
        public int    getPageCount()      { return pageCount;      }
        public String getFileSize()       { return fileSize;       }
        public long   getFileSizeBytes()  { return fileSizeBytes;  }
        public int    getWordCount()      { return wordCount;      }
        public String getAuthor()         { return author;         }
        public String getTitle()          { return title;          }
        public String getCreationDate()   { return creationDate;   }

        public void setFileName(String v)      { this.fileName      = v; }
        public void setPageCount(int v)        { this.pageCount     = v; }
        public void setFileSize(String v)      { this.fileSize      = v; }
        public void setFileSizeBytes(long v)   { this.fileSizeBytes = v; }
        public void setWordCount(int v)        { this.wordCount     = v; }
        public void setAuthor(String v)        { this.author        = v; }
        public void setTitle(String v)         { this.title         = v; }
        public void setCreationDate(String v)  { this.creationDate  = v; }
    }

    // ─────────────────────────────────────────────────────────────────
    // RÉSULTATS DE RECHERCHE
    // ─────────────────────────────────────────────────────────────────

    public static class SearchResultResponse {
        private int    pageNumber;
        private String context;
        private int    occurrences;

        public SearchResultResponse() {}

        public SearchResultResponse(int pageNumber, String context, int occurrences) {
            this.pageNumber  = pageNumber;
            this.context     = context;
            this.occurrences = occurrences;
        }

        public int    getPageNumber()  { return pageNumber;  }
        public String getContext()     { return context;     }
        public int    getOccurrences() { return occurrences; }

        public void setPageNumber(int v)  { this.pageNumber  = v; }
        public void setContext(String v)  { this.context     = v; }
        public void setOccurrences(int v) { this.occurrences = v; }
    }

    // ─────────────────────────────────────────────────────────────────
    // RÉPONSE FICHIER (download)
    // ─────────────────────────────────────────────────────────────────

    public static class FileResponse {
        private String downloadUrl;
        private String fileName;
        private long   sizeBytes;
        private String message;

        public FileResponse() {}

        public FileResponse(String downloadUrl, String fileName, long sizeBytes, String message) {
            this.downloadUrl = downloadUrl;
            this.fileName    = fileName;
            this.sizeBytes   = sizeBytes;
            this.message     = message;
        }

        public String getDownloadUrl() { return downloadUrl; }
        public String getFileName()    { return fileName;    }
        public long   getSizeBytes()   { return sizeBytes;   }
        public String getMessage()     { return message;     }

        public void setDownloadUrl(String v) { this.downloadUrl = v; }
        public void setFileName(String v)    { this.fileName    = v; }
        public void setSizeBytes(long v)     { this.sizeBytes   = v; }
        public void setMessage(String v)     { this.message     = v; }
    }

    // ─────────────────────────────────────────────────────────────────
    // STATUT DU SERVEUR
    // ─────────────────────────────────────────────────────────────────

    public static class ServerStatus {
        private boolean corbaRunning;
        private String  corbaInfo;
        private String  springVersion;
        private String  javaVersion;
        private String  serverTime;

        public ServerStatus() {}

        public boolean isCorbaRunning()   { return corbaRunning;   }
        public String  getCorbaInfo()     { return corbaInfo;      }
        public String  getSpringVersion() { return springVersion;  }
        public String  getJavaVersion()   { return javaVersion;    }
        public String  getServerTime()    { return serverTime;     }

        public void setCorbaRunning(boolean v)   { this.corbaRunning   = v; }
        public void setCorbaInfo(String v)       { this.corbaInfo      = v; }
        public void setSpringVersion(String v)   { this.springVersion  = v; }
        public void setJavaVersion(String v)     { this.javaVersion    = v; }
        public void setServerTime(String v)      { this.serverTime     = v; }
    }
}
