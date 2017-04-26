/**
  * Archive the auto-archived log folder
  */
def call() {
    def logFolder = getLogFolder()
    archiveArtifacts allowEmptyArchive: true, artifacts: "${logFolder}/**""
}