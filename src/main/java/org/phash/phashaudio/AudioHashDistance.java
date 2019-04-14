package org.phash.phashaudio;

/**
 * Public class for return info from 
 * the AudioHasher::ber method.  
 *
 **/
public class AudioHashDistance {
    int pos;           //position into hash the first hash was matched
    double cs;          //confidence score, 0-1

    public AudioHashDistance(){
		pos = -1;
		cs = 0.0;
    }
}
