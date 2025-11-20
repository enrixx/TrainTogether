package de.othr.traintogether.model.TrainingModel;

public class BodyMeasurements {

    public BodyMeasurements() {};

    private Measurement arm = new Measurement();
    private Measurement brust = new Measurement();
    private Measurement bein = new Measurement();
    private Measurement schulter = new Measurement();
    private Measurement ruecken = new Measurement();

    public Measurement getArm() { return arm; }
    public void setArm(Measurement arm) { this.arm = arm; }

    public Measurement getBrust() { return brust; }
    public void setBrust(Measurement brust) { this.brust = brust; }

    public Measurement getBein() { return bein; }
    public void setBein(Measurement bein) { this.bein = bein; }

    public Measurement getSchulter() { return schulter; }
    public void setSchulter(Measurement schulter) { this.schulter = schulter; }

    public Measurement getRuecken() { return ruecken; }
    public void setRuecken(Measurement ruecken) { this.ruecken = ruecken; }
}