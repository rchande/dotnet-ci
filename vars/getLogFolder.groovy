/**
  * Get the folder where logs should be placed to be automatically archived
  * @return Log folder.  Creates if necessary
  */
def call() {
    echo "Automatically archived log folder will be at ${WORKSPACE}/netci-archived-logs/"
    String logFolder = "${WORKSPACE}/netci-archived-logs/"
    if (isUnix()) {
        echo "if [ ! -d '${logFolder}' ]; then mkdir -p '${logFolder}'; fi"
        sh "if [ ! -d '${logFolder}' ]; then mkdir -p '${logFolder}'; fi"
    }
    else {
        bat "if NOT exists '${logFolder}' mkdir '${logFolder}'"
    }

    return logFolder
}