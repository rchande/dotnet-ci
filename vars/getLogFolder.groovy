/**
  * Get the folder where logs should be placed to be automatically archived
  * @return Log folder.  Creates if necessary
  */
def call() {
    String logFolder = "${WORKSPACE}/netci-archived-logs/"
    if (isUnix()) {
        sh "if [ ! -d '${logFolder}' ]; then mkdir -p '${logFolder}'"
    }
    else {
        bat "if NOT exists '${logFolder}' mkdir '${logFolder}'"
    }
    return logFolder
}