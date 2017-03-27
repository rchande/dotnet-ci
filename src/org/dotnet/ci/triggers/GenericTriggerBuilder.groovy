package org.dotnet.ci.triggers;

// Covers a generic triggers in Jenkins
// Periodic - Periodic triggers against a Git repo
class GenericTriggerBuilder {
    public enum TriggerType {
        PERIODIC
    }

    // Periodic
    // Cron string representing the execution period 
    private String cronString
    // Always run regardless of the commit state
    boolean alwaysRun = false

    // Periodic trigger setup

    // Constructs a new periodic trigger
    // Parameters:
    //  cronString - Cron string to run the job on
    // Returns:
    //  a new periodic trigger that runs on the specified interval
    def static TriggerBuilder triggerPeriodically(String cronString) {
        return new TriggerBuilder(TriggerType.PERIODIC)
    }
    
    // Forces the periodic trigger to run regardless of source change
    def alwaysRunPeriodicTrigger() {
        assert triggerType == TriggerType.PERIODIC
        assert !used
        this.alwaysRun = true
    }
}