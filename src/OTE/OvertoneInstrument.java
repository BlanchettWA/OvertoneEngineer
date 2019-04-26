package OTE;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.SineOscillator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;



/**
 * An instrument that models and simulates the interactions of sound waves and their parts (specifically partials). partials are the faster multiples that occur simultaneously with the oscillation of a wave. For example, a wave of 200 Hz will simulaneously create waves of 400 Hz (2nd OTE.Partial), 600 Hz(3rd OTE.Partial), 800 Hz(4th OTE.Partial), 10000 Hz (5th OTE.Partial), and so on
 *
 * @author William Blanchett
 * @version 0.9
 */
public class OvertoneInstrument
{
    private final DecimalFormat twoPlace = new DecimalFormat("#.##");

    // Constants for bounding values properly
    public final double MIN_AMPLITUDE = 0.0;
    public final double MAX_AMPLITUDE = 1.0;
    public final double MIN_PHASE = 0.0;
    public final double MAX_PHASE = 1.0;

    // Paremeters that affect the fundamental wave that the instrument is playing.
    private double frequency;
    private double basePhase;
    private double baseAmplitude;
    private SineOscillator baseWave;

    // Parameters that control and maintain the overall instrument.
    private LineOut speakerOutput;
    private List<Partial> partialWaves;
    private Synthesizer instrument;
    private boolean generatingSound;

    /**
     * Returns if this instrument is currently generating sound
     *
     * @return the current state of this instrument's sound generation
     */
    public boolean isPlaying()
    {
        return generatingSound;
    }

    /**
     * Commands this instrument to begin playing sound
     */
    public void generateSound()
    {
        baseWave.noteOn(frequency,baseAmplitude);

        for(Partial part: partialWaves)
        {
            part.generateSound();
        }
        generatingSound = true;
    }

    /**
     * Commands this instrument to stop playing sound
     */
    public void stopGeneratingSound()
    {
        baseWave.noteOff();

        for(Partial part: partialWaves)
        {
            part.stopGeneratingSound();
        }
        generatingSound = false;
    }

    /**
     * Creates a new instrument, specifying its initial base frequency and the overall volume of the instrument
     *
     * @param baseFrequency the initial frequency of the instrument's base wave. This is the frequency that all other sounds are based upon. The reasonable range is a range of hearing, or 20 Hz to 20,000 Hz for humans
     */
    public OvertoneInstrument(double baseFrequency)
    {
        this(baseFrequency,.90,0);
    }

    /**
     * Creates a new instrument, specifying the initial base frequency, the overall volume of the instrument, and the amplitude of the the instrument's base wave
     *
     * @param baseFrequency the initial frequency of the instrument's base wave. This is the frequency that all other sounds are based upon. The reasonable range is a range of hearing, or 20 Hz to 20,000 Hz for humans
     * @param baseAmplitude the amplitude of this instrument's base wave. Amplitude is the intensity of the wave, which isn't exactly the same as volume
     */
    public OvertoneInstrument(double baseFrequency,double baseAmplitude)
    {
        this(baseFrequency,baseAmplitude,0);
    }

    /**
     * Creates a new instrument, specifying the initial base frequency, the overall volume of the instrument, the amplitude of the the instrument's base wave, and the phase of this instrument's base wave
     *
     * @param baseFrequency the initial frequency of the instrument's base wave. This is the frequency that all other sounds are based upon. The reasonable range is a range of hearing, or 20 Hz to 20,000 Hz for humans
     * @param initialBaseAmplitude the amplitude of this instrument's base wave. Amplitude is the intensity of the wave, which isn't exactly the same as volume
     * @param initialBasePhase the starting position of the wave. This matters when combining different waves, becuase of the natural pulsing effect of sound waves
     */
    public OvertoneInstrument(double baseFrequency, double initialBaseAmplitude, double initialBasePhase)
    {
        this.frequency = baseFrequency;

        if (initialBaseAmplitude > MAX_AMPLITUDE){initialBaseAmplitude = MAX_AMPLITUDE;}
        if (initialBaseAmplitude < MIN_AMPLITUDE){initialBaseAmplitude = MIN_AMPLITUDE;}
        this.baseAmplitude = initialBaseAmplitude;

        if (initialBasePhase > MAX_PHASE){initialBasePhase = MAX_PHASE;}
        if (initialBasePhase < MIN_PHASE){initialBasePhase = MIN_PHASE;}
        this.basePhase = initialBasePhase;


        this.instrument = JSyn.createSynthesizer(); //The required underlying engine for all of the waves
        this.speakerOutput = new LineOut(); //Interface for the device speakers

        this.partialWaves = new ArrayList<Partial>(); //List of the partials of the instrument.

        this.instrument.add(speakerOutput); //Register the speaker output with the Instrument Synthesizer
        this.instrument.start(); //Required action so that sound can be heard.
        this.speakerOutput.start(); //Required action for sound to be heard

        this.baseWave = new SineOscillator(baseFrequency); //Base tone of the instrument.
        this.instrument.add(this.baseWave); //Add into the Synthesizer that controlls everything
        this.baseWave.output.connect(0,this.speakerOutput.input,0); //Add the wave's output to the speakers. This needs to be done twice so that the sound is stereo.
        this.baseWave.output.connect(0,this.speakerOutput.input,1);

        //Apply the initial parameters
        this.baseWave.amplitude.set(this.baseAmplitude);
        this.baseWave.phase.set(this.basePhase);
        this.baseWave.frequency.set(this.frequency);
        this.baseWave.noteOff(); //So the instrument can start quietly.
        this.generatingSound = false;
    }

    /**
     * Check if this instrument has a partial of the specified degree. This is an internal function used for processes
     *
     * @param questionDegree the degree that is being checked for
     * @return the partial of the specified degree, null if that partial does not yet exist
     */
    private Partial waveExists(int questionDegree)
    {
        Partial target = null;

        for (Partial part: partialWaves)
        {
            if (part.getPartialNumber() == questionDegree)
            {
                target = part;
            }
        }

        return target;
    }

    /**
     * Adjusts the detune of the partial with the specified degree. The detune is how far away the partial is from a perfect multiple of the base frequency
     *
     * @param degree the degree of the partial that will be modified. This should be a partial that has already been created in the instrument
     * @param newDetune the new detune of the specified partial. This can be be any value (positive or negative). Reasonable detunes are generally -15 to 15, but experimentation leads to interesting results
     * @throws PartialError the specified partial does not exist in this instrument
     */
    public void adjustPartialDetune(int degree, double newDetune) throws PartialError
    {
        Partial selected = waveExists(degree);
        if (selected == null) { throw new PartialError("OTE.Partial degree doesn't Exist!"); }
        else { selected.adjustDetune(newDetune);
            if (!this.generatingSound){selected.getOscillator().noteOff();}}
    }

    /**
     * Adjusts the amplitude of the partial with the specified degree. The amplitude signifies the intensity of the wave
     *
     * @param degree the degree of the partial that will be modified. This should be a partial that has already been created in the instrument
     * @param newAmplitude the new amplitude of the spedified partial. This must be a double bewteen 0.0 and 1.0
     * @throws PartialError the speficied partial does not exist in this instrument
     */
    public void adjustPartialAmplitude(int degree, double newAmplitude) throws PartialError
    {
        Partial selected = waveExists(degree);
        if (selected == null) { throw new PartialError("OTE.Partial degree doesn't exist!"); }
        else
        {
            if (newAmplitude > MAX_AMPLITUDE) {newAmplitude = MAX_AMPLITUDE;}
            if (newAmplitude < MIN_AMPLITUDE) {newAmplitude = MIN_AMPLITUDE;}
            selected.adjustAmplitude(newAmplitude);
            if (!this.generatingSound){selected.getOscillator().noteOff();}
        }
    }

    /**
     * Adjusts the phase of the partial with the specified degree. The phase indicates the wave's position at specific times
     *
     * @param degree the degree of the partial that will be modified. This should be a partial that has already been created in the instrument
     * @param newPhase the new phase of the specified partial. This must be a double between 0.0 and 1.0
     * @throws PartialError the specified partial does not exist in this instrument
     */
    public void adjustPartialPhase(int degree, double newPhase) throws PartialError
    {
        Partial selected = waveExists(degree);
        if (selected == null) { throw new PartialError("OTE.Partial degree doesn't exist!"); }
        else
        {
            if (newPhase > MAX_PHASE) {newPhase = MAX_PHASE;}
            if (newPhase < MIN_PHASE) {newPhase = MIN_PHASE;}
            selected.adjustPhase(newPhase);
            if (!this.generatingSound){selected.getOscillator().noteOff();}
        }
    }

    /**
     * Returns the frequency of the partial with the specified degree. This is the frequency that the partial will be sounding at
     *
     * @param degree the degree of the partial that will have its frequency returned. Ths should be an partial that has already been created in the instrument
     * @return the frequency of the partial with the specified degree
     * @throws PartialError the specified partial does not exist in this instrument
     */
    public double getPartialFrequency(int degree) throws PartialError
    {
        Partial selected = waveExists(degree);
        if (selected == null) { throw new PartialError("OTE.Partial degree doesn't exist!"); }
        else { return selected.getFrequency();
        }
    }

    /**
     * Returns the detune of the partial with the specified degree. This is the difference of the partial from a perfect multiplicity
     *
     * @param degree the degree of the partial that will have its detune returned. This should be an partial that has already been created in the instrument
     * @return the detune of the partial with the specified degree
     * @throws PartialError the specified partial does not exist in this instrument
     */
    public double getPartialDetune(int degree) throws PartialError
    {
        Partial selected = waveExists(degree);
        if (selected == null)
        {
            throw new PartialError("OTE.Partial degree doesn't exist!");
        }
        else
        {
            return selected.getDetune();
        }
    }

    /**
     * Returns the amplitude of the partial with the specified degree. This is the intensity of the wave
     *
     * @param degree the degree of the partial that will have its amplitude returned. This should be an partial that has already been created in the instrument
     * @return the amplitude of the partial with the specified degree
     * @throws PartialError the specified partial does not exist in this instrument
     */
    public double getPartialAmplitude(int degree) throws PartialError
    {
        Partial selected = waveExists(degree);
        if (selected == null)
        {
            throw new PartialError("OTE.Partial Degree doesn't exist!");
        }

        else
        {
            return selected.getAmplitude();
        }

    }

    /**
     *Returns the phase of the phase of the partial with the specified degree. This is the wave's position in time
     *
     * @param degree the degree of the partial that will have its phase returned. This should be an partial that has already been created in the instrument
     * @return the phase of the partial with the specified degree
     * @throws PartialError the specified partial does not exist in this instrument
     */
    public double getPartialPhase(int degree) throws PartialError
    {
        Partial selected = waveExists(degree);
        if (selected == null)
        {
            throw new PartialError("OTE.Partial Degree doesn't exist!");
        }

        else
        {
            return selected.getPhase();
        }
    }

    /**
     * Returns the list of degrees with created partial in this instrument
     *
     * @return the list of degrees that exist in the instrument
     */
    public int[] getPartialDegrees()
    {
        int[] validList = new int[partialWaves.size()];
        int i = 0;

        for (Partial part : partialWaves)
        {
            validList[i] = part.getPartialNumber();
        }

        return validList;
    }

    /**
     * Adjusts the frequency of the overall instrument
     *
     * @param newFrequency the new base frequency of the instrument
     */
    public void adjustFrequency(double newFrequency)
    {
        this.frequency = newFrequency;
        baseWave.frequency.set(this.frequency);

        for (Partial part: partialWaves)
        {
            part.adjustFrequency(this.frequency);
        }
        if (!this.generatingSound){stopGeneratingSound();}
    }

    /**
     * Adjusts the frequency of the overall instrument, using a note name (like A3, C4, Gb2)
     *
     * @param newNote the new base note of the instrument.
     * @throws Exception The note name given was not found or recgonized
     */

    public void adjustPitch(String newNote) throws Exception {
        try {
            adjustFrequency(Tools.NotetoFrequency(newNote));
        }
        catch (Exception e)
        {
            throw e;
        }
    }


    /**
     * Returns the frequency of the overall instrument
     *
     * @return the base frequency of the instrument
     */
    public double getFrequency(){return frequency;}


    /**
     * Creates and adds an partial of the specified degree
     *
     * @param degree the degree of the partial that will be created. The degree must be an integer greater than 1
     * @throws PartialError the specified partial already exists
     */
    public void addPartial(int degree) throws PartialError {this.addPartial(degree,0,.75,0);}

    /**
     * Creates and adds an partial of the specified degree with the specified detune
     *
     * @param degree the degree of the partial that will be created. The degree must be an integer greater than 1
     * @param detune the detune of the partail that will be created. This can be be any value (positive or negative). Reasonable detunes are generally -15 to 15, but experimentation leads to interesting results
     * @throws PartialError the specified partial already exists
     */
    public void addPartial(int degree, double detune) throws PartialError {this.addPartial(degree,detune,.75,0);}

    /**
     *  Creates and adds an partial of the specified degree with the specified detune and specified amplitude
     *
     * @param degree the degree of the partial that will be created. The degree must be an integer greater than 1
     * @param detune the detune of the partial that will be created. This can be be any value (positive or negative). Reasonable detunes are generally -15 to 15, but experimentation leads to interesting results
     * @param amplitude the amplitude of the partial that will be created. This must be between 0.0 and 1.0
     * @throws PartialError the specified partial already exists
     */
    public void addPartial(int degree, double detune, double amplitude) throws PartialError {this.addPartial(degree,detune,amplitude,0);}

    /**
     * Creates and adds an partial of the specified degree with the specified detune, specified amplitude, and specified phase
     *
     * @param degree the degree of the partial that will be created. The degree must be an integer greater than 1
     * @param detune the detune of the partial that will be created. This can be be any value (positive or negative). Reasonable detunes are generally -15 to 15, but experimentation leads to interesting results
     * @param amplitude the amplitude of the partial that will be created. This must be between 0.0 and 1.0
     * @param phase the phase of the partial that will be created. This must be between 0.0 and 1.0
     * @throws PartialError the specified partial already exists
     */
    public void addPartial(int degree, double detune, double amplitude, double phase) throws PartialError
    {
        if (!(waveExists(degree) == null))
        {
            throw new PartialError("Degree already exists!");
        }
        else
        {
            partialWaves.add(new Partial(degree,instrument,speakerOutput,frequency,detune,amplitude,phase));

            if (generatingSound)
            {
                partialWaves.get(partialWaves.size()-1).generateSound();
            }
        }

    }


    /**
     * Removes the the partial with the specified degree from the instrument
     *
     * @param degree the degree of the partial that will be removed. The degree must be an partial that already exists in this instrument
     * @throws PartialError specified partial does not exist in this instrument
     */
    public void removePartial(int degree) throws PartialError
    {
        Partial selected = waveExists(degree);

        if (selected == null)
        {
            throw new PartialError("OTE.Partial does not exist!");
        }
        else
        {
            selected.stopGeneratingSound();
            instrument.remove(selected.getOscillator());
            partialWaves.remove(selected);

            // instrument.remove(partialWaves.get(iterator).getOscillator());
            // partialWaves.remove(iterator);
        }
    }

    /**
     * Removes all partial from the instrument
     */
    public void removeAllPartials()
    {
        while (partialWaves.size() > 0)
        {
            partialWaves.get(0).stopGeneratingSound();
            instrument.remove(partialWaves.remove(0).getOscillator());
        }
    }

    //TODO: Flesh out and write
    public String getInfo(){return "Information?";}

    /**
     * Adjusts the amplitude of the instrument's base wave. This is different from the instrument's volume
     *
     * @param newAmplitude the new amplitude of the instrument's base wave. This must be between 0.0 and 1.0
     */
    public void adjustBaseAmplitude(double newAmplitude)
    {
        if (newAmplitude > MAX_AMPLITUDE)
        {
            newAmplitude = MAX_AMPLITUDE;
        }

        if (newAmplitude < MIN_AMPLITUDE){newAmplitude = MIN_AMPLITUDE;}

        this.baseAmplitude = newAmplitude;
        this.baseWave.amplitude.set(this.baseAmplitude);
        if (!this.generatingSound){baseWave.noteOff();}
    }


    /**
     * Adjusts the phase of the instrument's base wave
     *
     * @param newPhase the new phase of the instrument's base wave. This must be between 0.0 and 1.0
     */
    public void adjustBasePhase(double newPhase)
    {
        if (newPhase > MAX_PHASE)
        {
            newPhase = MAX_PHASE;
        }

        if (newPhase < MIN_PHASE){newPhase = MIN_PHASE;}

        this.basePhase = newPhase;
        this.baseWave.phase.set(this.basePhase);
        if (!this.generatingSound){baseWave.noteOff();}
    }

}

/**
 * This class simulates a partial, the simultaneous higher frequency sound wave that naturally results in any sound
 */
class Partial
{
    private final DecimalFormat twoPlace = new DecimalFormat("#.##");

    private double baseFrequency;

    private double detune;
    private double partialFrequency;
    private double partialAmplitude;
    private double partialPhase;
    private int partialNumber;

    private SineOscillator toneGen;

    public Partial(int degree, Synthesizer baseSynth, LineOut baseSpeaker, double baseFrequency, double initialDetune, double amplitude) throws PartialError
    {
        this(degree,baseSynth,baseSpeaker,baseFrequency,initialDetune,amplitude,0);
    }

    public Partial(int degree, Synthesizer baseSynth, LineOut baseSpeaker, double baseFrequency, double initialDetune) throws PartialError
    {
        this(degree,baseSynth,baseSpeaker,baseFrequency,initialDetune,.75,0);
    }

    public Partial(int degree, Synthesizer baseSynth, LineOut baseSpeaker, double baseFrequency, double initialDetune, double amplitude, double initialPhase) throws PartialError {

        if (!(degree > 1)) {throw new PartialError("Invalid OTE.Partial Degree. Degree must be greater than 1");}

        this.partialNumber = degree;

        this.baseFrequency = baseFrequency;
        this.detune = initialDetune;
        this.partialFrequency = ((this.baseFrequency * this.partialNumber) +  this.detune);
        this.partialAmplitude = amplitude;
        this.partialPhase = initialPhase;

        this.toneGen = new SineOscillator();
        baseSynth.add(toneGen);
        this.toneGen.output.connect(0,baseSpeaker.input,0);
        this.toneGen.output.connect(0,baseSpeaker.input,1);

        this.toneGen.frequency.set(this.partialFrequency);
        this.toneGen.amplitude.set(this.partialAmplitude);
        this.toneGen.phase.set(this.partialPhase);
        this.toneGen.noteOff();
    }


    public void adjustDetune(double newDetune)
    {
        this.detune = newDetune;
        this.partialFrequency = ((this.baseFrequency * this.partialNumber) + this.detune);
        this.toneGen.frequency.set(this.partialFrequency);
    }


    public double getDetune(){return detune;}


    public void adjustFrequency(double newBaseFrequency)
    {
        this.baseFrequency = newBaseFrequency;
        this.partialFrequency = ((this.baseFrequency * this.partialNumber) + this.detune);
        this.toneGen.frequency.set(this.partialFrequency);
    }


    public double getFrequency(){return partialFrequency;}


    public void adjustAmplitude(double newAmplitude)
    {
        if (newAmplitude > 1.0) { newAmplitude = 1; }
        else if (newAmplitude < 0) { newAmplitude = 0; }

        this.partialAmplitude = newAmplitude;
        this.toneGen.amplitude.set(this.partialAmplitude);
    }

    public double getAmplitude(){return partialAmplitude;}


    public void adjustPhase(double newPhase)
    {
        this.partialPhase = newPhase;
        this.toneGen.phase.set(this.partialPhase);
    }


    public double getPhase(){return partialPhase;}


    public int getPartialNumber(){return partialNumber;}


    public String getInfo(char flag)
    {
        String returnValue;

        switch (flag)
        {
            case 'p':
                returnValue = String.format("OTE.Partial of the %d Degree for base frequency %f Hz. Generating sound at a frequency of %f Hz, with a detune of %f Hz. Amplitude of %f/1.0 and Phase of %f. \n",this.partialNumber,this.baseFrequency,this.partialFrequency,this.detune,this.partialAmplitude,this.partialPhase);
                break;


            case 'l':
                returnValue = String.format("OTE.Partial Degree: %d | Base Frequency: %s Hz |  OTE.Partial Frequency: %s Hz | Detune: %s Hz | Amplitude: %s/1.0 | Phase: %s. \n",this.partialNumber,twoPlace.format(this.baseFrequency),twoPlace.format(this.partialFrequency),twoPlace.format(this.detune),twoPlace.format(this.partialAmplitude),twoPlace.format(this.partialPhase));
                break;

            case 's':
                returnValue = String.format("OTE.Partial Degree: %d | Base Frequency: %s Hz |  OTE.Partial Frequency: %s Hz | Detune: %s Hz\n",this.partialNumber,twoPlace.format(this.baseFrequency),twoPlace.format(this.partialFrequency),twoPlace.format(this.detune));
                break;

            default:
                returnValue = String.format("OTE.Partial Degree: %d \nBase Frequency: %s Hz \nOTE.Partial Frequency: %s Hz \nDetune: %s Hz \nAmplitude: %s/1.0 \nPhase: %s. \n\n",this.partialNumber,twoPlace.format(this.baseFrequency),twoPlace.format(this.partialFrequency),twoPlace.format(this.detune),twoPlace.format(this.partialAmplitude),twoPlace.format(this.partialPhase));
                break;

        }
        return returnValue;
    }


    public void generateSound()
    {
        this.toneGen.noteOn(this.partialFrequency,(this.partialAmplitude));
    }

    public void stopGeneratingSound()
    {
        this.toneGen.noteOff();
    }


    public SineOscillator getOscillator(){return this.toneGen;}

}

class PartialError extends Exception
{
    public PartialError(String message) {super(message);}

    public PartialError(Throwable cause) {super((cause));}

    public PartialError(String message, Throwable cause) {super(message, cause);}
}



