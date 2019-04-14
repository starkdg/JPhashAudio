package org.phash.phashaudio;

/**
 * Public class for return info from 
 * the AudioHasher::calc method.  
 *
 **/
public class AudioHashInfo {

    /** Array of hash values.
     * <p>int hash array denoting the 
     * fingerprint of audio signal.
     * 1D array expected to be nbframes
     * in length.</p>
     **/
    public int[] hasharray = null;

    /** bit positions to toggle in array of hash values.
     * <p>Bit positions of the bits
     * most likely to flip in
     * audio distortion.  Size of array
     * expected to be nbframes length</p>
     * Array index is right to left: 31,30,...,1,0
     **/
    public int[] toggles = null;

    /** Frequency coefficients for each frame.
     * <p>frequency coefficients for each frame
     * of given signal.  Size of 2D array will be nbframesx33.</p>
     **/
    public double[][] coeffs = null;
 }