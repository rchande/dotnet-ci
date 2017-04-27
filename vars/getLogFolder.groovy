/**
  * Get the folder where logs should be placed to be automatically archived
  * @return Log folder.  Creates if necessary
  */
def call() {
    String logFolder
    if (isUnix()) {
        logFolder = "${WORKSPACE}/netci-archived-logs/"
        sh "mkdir -p '${logFolder}'"
    }
    else {
        logFolder = "${WORKSPACE}\\netci-archived-logs\\"
        assert logFolder.indexOf("/") == -1 : "Unexpected forward slashes in windows path component"
        bat "if NOT exists '${logFolder}' mkdir '${logFolder}'"
    }

    return logFolder
}