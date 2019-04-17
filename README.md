# JPHashAudio

An audio fingerprint identification algorithm. This is a java package with functions
to extract a perceptual hash from an audio signal.  There is also functions to query
the auscoutd server, which can be found here: [auscoutd](https://github.com/starkdg/JAudioScout)

This package is intended to provide everything you need to design and build an AudioScout
client application - to query the server, submit new audio fingerprints, etc.  Check out the
javadocs jar for the full documentation.

# Install

To install package in local maven repository:

```
mvn package test install
```


# Example Usage

Its quite simple.  To calculate two hashes and compare them,
just do this:

```java
	import org.phash.phashaudio.AudioHasher

	int sample_rate = 8000;
	int p = 4;
	
	AudioHasher hasher = new AudioHasher(sample_rate);

	AudioHashInfo hash = hasher.calc(buf, p);

	AudioHashInfo hash2 = hasher.calc(buf2, p);
	
	AudioHashDistance distance = hasher.audioHash_distance_ber(hash, hash2);
	System.out.println("pos = %d\n", distance.pos);
	System.out.println("confidence score = %.4f\n", distance.cs);
```

You can also use the `QuerySender` to query the auscoutd server:

```java
	import org.phash.phashaudio.QuerySender;
	import org.phash.phashaudio.MatchResult;
	
	String address = "http://localhost:4005";
	int n_threads = 1;
	QuerySender sender = new QuerySender(address, n_threads);

	float threshold = 0.075;
	int bs = 128;
	int timeout = 500;
	List<MatchResult> results = sender.sendQuery(hash.hasharray, hash.toggles, threshold, bs, timeout);

```

Audio signal can be read from files using the [JAudioData](https://github.com/starkdg/libAudioData)
java bindings.







