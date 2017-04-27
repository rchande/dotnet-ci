/**
  * Archive the auto-archived log folder
  */
def call() {
    String logFolder = getLogFolder()
    archiveArtifacts allowEmptyArchive: true, artifacts: "${logFolder}/**"
}