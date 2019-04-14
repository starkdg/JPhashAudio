package org.phash.phashaudio;

import java.util.Arrays;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class AudioHasher {

    protected class FreqDiff {
		public Double diff;
		public Integer index;
		public FreqDiff(){
			this.diff = 0.0;
			this.index = 0;
		}
		public FreqDiff(double diff, int index){
			this.diff = diff;
			this.index = index;
		}
    }

    final protected int nfilts = 33;

    final protected double frameDuration = 0.40; /* 0.40 seconds per frame */

    final protected float freqWidth = 1.06f;

	final protected float[] freqs = {  50.0f,  75.0f,  100.0f, 125.0f,   150.0f,  200.0f,  250.0f,  300.0f,
				     350.0f, 400.0f,  450.0f, 510.0f,   570.0f,  635.0f,  700.0f,  770.0f,
                                     840.0f, 920.0f, 1000.0f, 1085.0f, 1170.0f, 1270.0f, 1370.0f, 1485.0f,
                                    1600.0f,1725.0f, 1850.0f, 2000.0f, 2150.0f, 2325.0f, 2500.0f, 2700.0f,
                                    2900.0f };

	protected int framelength, nffthalf, overlap, advance;

	protected double maxFreq;

	protected DoubleFFT_1D fftTransform;

    protected double[] frame;
    protected double[] magnF;
    protected double[][] wts;
    protected double[] window;
    protected FreqDiff[] freqdiffs;

    public AudioHasher(int sr){
		
		this.framelength = getFrameLength(frameDuration, sr);
		this.nffthalf = framelength/2;
		this.overlap = (int)(31*framelength/32);
		this.advance = framelength - overlap;
		this.maxFreq = (double)(sr/2);
		this.fftTransform = new DoubleFFT_1D(framelength);
		this.frame = new double[framelength];
		this.magnF = new double[nffthalf];
		this.wts = createFilters(nffthalf);
		this.window = createHammingWindow(framelength);
		this.freqdiffs = new FreqDiff[nfilts-1];
		for (int i=0;i<nfilts-1;i++){
			freqdiffs[i] = new FreqDiff();
		}
    }

    public int getFrameLength(double dur, int sr){
		int nbsamples = (int)(dur*(float)sr);
		return Integer.highestOneBit(nbsamples);
    }

    private double[] createHammingWindow(int fl){
		double[] window = new double[fl];
		for (int i=0;i<fl;i++){
			window[i] = 0.54 - 0.46*Math.cos(2*Math.PI*i/(fl-1));
		}
		return window;
    }

    private double[][] createFilters(int len){
		double[][] wts = new double[nfilts][len];
		double[] binbarks = new double[len];

		double f_bark_mid, mdouble, bark_diff, lof, hif;
		for (int i=0;i < len;i++){
			double temp = i*maxFreq/(float)len;
			temp /= 600.0;
			binbarks[i] = 6*Math.log(temp + Math.sqrt(temp*temp + 1.0));
		}

		for (int i=0;i < nfilts;i++){
			f_bark_mid = freqs[i]/600.0f;
			f_bark_mid = 6*Math.log(f_bark_mid + Math.sqrt(f_bark_mid*f_bark_mid + 1.0));
			for (int j=0;j < len;j++){
				bark_diff = binbarks[j] - f_bark_mid;
				lof = -2.5*(bark_diff/freqWidth - 0.5);
				hif = bark_diff/freqWidth + 0.5f;
				mdouble = (lof < hif) ? lof : hif;
				mdouble = (mdouble < 0.0f) ? mdouble : 0.0f;
				mdouble = Math.pow(10, mdouble);
				wts[i][j] = mdouble;
			}
		}

		return wts;
    }

    private void sortFreqDiffs(FreqDiff[] diffs){
		int minpos;
		for (int i=0;i < diffs.length;i++){
			minpos = i;
			for (int j=i+1;j < diffs.length;j++){
				if (diffs[j].diff < diffs[minpos].diff){
					minpos = j;
				}
			}
			if (i != minpos){
				Double tmpdiff = diffs[i].diff;
				diffs[i].diff = diffs[minpos].diff;
				diffs[minpos].diff = tmpdiff;
				
				Integer tmpindex = diffs[i].index;
				diffs[i].index = diffs[minpos].index;
				diffs[minpos].index = tmpindex;
			}
		}
    }

    /** calculate audio hash for a buffer of audio samples
     * @param buffer float[] buffer of audio samples
     * @param p no. toggles to consider
     * @return AudioHashInfo
     **/
    public AudioHashInfo calc(float[] buffer, int p){
		int start = 0;
		int end = start + framelength - 1;
        int totalframes = (int)(Math.floor(buffer.length/advance - Math.floor(framelength/advance) + 1));
		int nbhashes = totalframes - 2;
		int nbtoggles = (p <= 12) ? p : 12;

		AudioHashInfo hashresult = new AudioHashInfo();
		hashresult.hasharray = new int[nbhashes];
		hashresult.toggles = (p > 0) ? new int[nbhashes] : null;
		hashresult.coeffs = new double[totalframes][nfilts];
		
		int index = 0;
		double maxCoeff;
		while (end < buffer.length){
			maxCoeff = 0.0;
			
			//  apply hamming window to frame
			for (int i=0;i < framelength;i++){
				frame[i] = window[i]*(double)buffer[start+i];
			}
			
			// forward fft transform
			fftTransform.realForward(frame);
			
			magnF[0] = frame[0];
			for (int i=1;i < nffthalf-1;i++){
				magnF[i] = (float)Math.abs(Math.sqrt((Math.pow(frame[2*i], 2) 
													  + Math.pow(frame[2*i+1], 2))));
			}
			magnF[nffthalf-1] = frame[1];
			
			// critical band integration
			for (int i=0;i<nfilts;i++){
				hashresult.coeffs[index][i] = 0.0;
				for (int j=0;j < nffthalf;j++){
					hashresult.coeffs[index][i] += wts[i][j]*magnF[j];
				}
			}
			
			index += 1;
			start += advance;
			end += advance;
		}

		index = 0;
		for (int i=1;i<totalframes - 1;i++){
			int hashvalue = 0;
			for (int m=0;m<nfilts-1;m++){
				double diff = (hashresult.coeffs[i+1][m] - hashresult.coeffs[i+1][m+1]) - 
					(hashresult.coeffs[i-1][m] - hashresult.coeffs[i-1][m+1]);
				hashvalue <<= 1;
				if (diff > 0) hashvalue |= 0x01;
				freqdiffs[m].diff = Math.floor(Math.abs(diff));
				freqdiffs[m].index = m;
			}
			hashresult.hasharray[index] = hashvalue;
			if (p > 0){
				int tog = 0;
				sortFreqDiffs(freqdiffs);
				for (int j=0;j<nbtoggles;j++){
					tog |= (0x80000000 >>> freqdiffs[j].index);
				}
				hashresult.toggles[index] = tog;
			}
			index++;
		}
		
		return hashresult;
    }

    /** convenience function to calculate no. seconds of offset found
     *  with distance_ber function
     *  @param sr sample rate
     *  @param pos int - match position found from ber function
     *  @return float value for no. seconds
     **/
    public float getSecsOffset(int sr, int pos){

        return (float)(framelength*pos)/(float)(32*sr);
		
    }

    protected double compare_blocks(int[] blocka, int[] toggles, int[] blockb){
		double result = 0;
		for (int i=0;i < blocka.length;i++){
			int bitmask = (toggles != null && toggles.length > 0) ? toggles[i] : 0;
			int xordhash = (blocka[i]^blockb[i]) & (~bitmask);
			result += Integer.bitCount(xordhash);
		}
		result /= (double)(32*blocka.length);
		return result;
    }

    /** find best position in which a smaller hash array matches against a longer hash array
     *  @param hash1 AudioHashInfo object for first shorter hash (with optional toggles info
     *  @param hash2 AudioHashInfo object for second longer hash (without toggles)
     *  @return AudioHashDistance object
     *  @throws IllegalArgument exception if first hash array is longer than second
     **/
    public AudioHashDistance audiohash_distance_ber(AudioHashInfo hash1, AudioHashInfo hash2){
		if (hash1.hasharray.length > hash2.hasharray.length)
			throw new IllegalArgumentException("hash1 cannot be longer than hash2");

		int Na = hash1.hasharray.length;
		int Nb = hash2.hasharray.length;
		int Nc = Nb - Na + 1;
		
		AudioHashDistance result = new AudioHashDistance();
		double dist;
		for (int i=0; i < Nc;i++){
			int[] subhash2 = Arrays.copyOfRange(hash2.hasharray, i, i+Na);
			dist = 1.0 - compare_blocks(hash1.hasharray, hash1.toggles, subhash2);
			if (dist > result.cs){
				result.cs = dist;
				result.pos = i; 
			}
		}
		return result;
    }
}
