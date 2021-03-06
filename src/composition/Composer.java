package composition;

import java.util.HashMap;

import instrument.Chord;
import instrument.ChordProgression;
import instrument.Instrument;
import instrument.Instrument.InstrumentType;
import instrument.InstrumentFactory;
import music.MusicGenerator;
import music.RandomUtil;
import music.Waves;
import music.Waves.WaveType;

public class Composer {

	MusicGenerator generator;
	RandomUtil rand;
	HashMap<InstrumentType, WaveType> instrumentWaves;

	public Composer(MusicGenerator generator) {
		this.generator = generator;
		this.rand = new RandomUtil();
	}

	public byte[] compose() {

		//Set the wave types for each instrument
		instrumentWaves = new HashMap<InstrumentType, WaveType>();
		WaveType instWaves[] = new Waves.WaveType[] { WaveType.sin, WaveType.saw, WaveType.square, WaveType.triangle,
				WaveType.aa };
		for (InstrumentType type : InstrumentType.values()) {
			instrumentWaves.put(type, instWaves[rand.nextInt(instWaves.length)]);
		}

		double secondsPerBeat = rand.nextDouble() + .2;
		double chanceToChangeTime = .25; //Probability a section will diverge from secondsPerBeat

		Chord mainTonic = new Chord(rand.nextInt(100) + 100,
				Chord.ChordType.values()[rand.nextInt(Chord.ChordType.values().length)]);

		Section sections[] = new Section[rand.nextInt(5) + 4];

		int measuresInSection = rand.nextEven(2, 8);
		int beatsPerMeasure = rand.nextInt(7) + 3;
		int subdivide = beatsPerMeasure * (rand.nextInt(3) + 3);
		sections[0] = intro(new Time(secondsPerBeat, beatsPerMeasure, subdivide, measuresInSection), mainTonic);
		for (int i = 1; i < sections.length - 1; i++) {
			int id = rand.nextInt(sections.length / 2 + 1);
			for (int j = 0; j < i; j++) {
				if (sections[j].id == id) {
					sections[i] = sections[j].duplicate();
					sections[i].startVolume = rand.nextInt(100) + 50;
					sections[i].endVolume = rand.nextInt(100) + 50;
					sections[i].complexify();
				}
			}
			if (sections[i] == null) {
				measuresInSection = rand.nextEven(8, 16);
				beatsPerMeasure = rand.nextInt(7) + 3;
				subdivide = beatsPerMeasure * (rand.nextInt(3) + 3);
				double spb = secondsPerBeat;
				if (rand.nextDouble() < chanceToChangeTime) {
					spb = rand.nextDouble() + .2;
				}
				Time time = new Time(spb, beatsPerMeasure, subdivide, measuresInSection);

				sections[i] = new Section(id, time);

				Chord tonic = new Chord(mainTonic.getScaleNoteHz()[rand.nextInt(mainTonic.getScaleNoteHz().length)],
						Chord.ChordType.values()[rand.nextInt(Chord.ChordType.values().length)]);

				ChordProgression progression = new ChordProgression(sections[i].time, tonic,
						rand.nextDouble() * sections[i].time.beatsPerMeasure, rand.nextInt(8));

				InstrumentFactory factory = new InstrumentFactory(time, progression, instrumentWaves);
				for (InstrumentType type : InstrumentType.values()) {
					Instrument inst = factory.getInstrument(type, 1 - (rand.nextDouble() / 4));
					sections[i].addInstrument(inst);
				}

				sections[i].startVolume = rand.nextInt(100) + 50;
				sections[i].endVolume = rand.nextInt(100) + 50;
				sections[i].randomizeBalances();
			}
		}
		measuresInSection = rand.nextEven(2, 6);
		beatsPerMeasure = rand.nextInt(7) + 3;
		subdivide = beatsPerMeasure * (rand.nextInt(3) + 3);
		sections[sections.length - 1] = outro(new Time(secondsPerBeat, beatsPerMeasure, subdivide, measuresInSection),
				mainTonic);

		return generator.getMusic(sections);
	}

	private Instrument withDelay(InstrumentFactory factory, Time time, InstrumentType type, int delayMeasures) {
		time = new Time(time.secondsPerBeat, time.beatsPerMeasure, time.subdivide, time.numMeasures - delayMeasures);
		ChordProgression progression = new ChordProgression(factory.getChordProgression(), delayMeasures);
		return factory.getInstrument(type, time, progression, 1 - (rand.nextDouble() / 4));
	}

	private Section intro(Time time, Chord tonic) {
		// We'll be kinda lame and restrict ourselves to a layered intro with an
		// integer number of measures between
		// instrument additions. TODO: Change later to make more interesting!!!
		Section section = new Section(100, time); // an ID that won't be chosen
													// later
		int maxDelay = time.numMeasures;
		ChordProgression progression = new ChordProgression(time, tonic, rand.nextDouble() * time.beatsPerMeasure,
				rand.nextInt(8));
		InstrumentFactory factory = new InstrumentFactory(time, progression, instrumentWaves);
		InstrumentType start = InstrumentType.values()[rand.nextInt(InstrumentType.values().length)];
		section.addInstrument(withDelay(factory, time, start, 0)); 
		for (InstrumentType type : InstrumentType.values()) {
			if (type == start)
				continue;
			int delay = rand.nextInt(maxDelay);
			Instrument inst = withDelay(factory, time, type, delay);
			section.addInstrument(inst, delay);
		}
		section.startVolume = rand.nextInt(100) + 50;
		section.endVolume = rand.nextInt(100) + 50;
		section.randomizeBalances();
		return section;
	}

	private Section outro(Time time, Chord tonic) {
		Section section = new Section(101, time);

		ChordProgression progression = new ChordProgression(section.time, tonic,
				rand.nextDouble() * section.time.beatsPerMeasure, rand.nextInt(8));

		InstrumentFactory factory = new InstrumentFactory(time, progression, instrumentWaves);
		for (InstrumentType type : InstrumentType.values()) {
			Instrument inst = factory.getInstrument(type, 1 - (rand.nextDouble() / 4));
			section.addInstrument(inst);
		}

		section.startVolume = rand.nextInt(100) + 50;
		section.endVolume = 0;
		section.randomizeBalances();
		return section;
	}

}
