/**
  * Get the folder where logs should be placed to be automatically archived
  * @return Log folder.  Creates if necessary
  */
def call() {
    if (isUnix()) {
        def logFolder = "${WORKSPACE}/netci-archived-logs/"
        sh "if [ ! -d '${logFolder}' ]; then mkdir -p '${logFolder}'"
    }
    else {
        bat "if NOT exists '${logFolder}' mkdir '${logFolder}'"
    }
    return logFolder
}