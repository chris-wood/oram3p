package sprout.ui;

import sprout.util.Util;

import java.math.BigInteger;
import java.security.SecureRandom;

public class EvictionTest
{
	public static void main(String[] args) throws Exception {		
		SecureRandom rnd = new SecureRandom();
		
		// parameter
		int i 				= 2;
		int d_i				= 4;
		int d_ip1 			= 7;
		int tau 			= 3;
		int twotaupow 		= (int) Math.pow(2, tau);
		int ln 				= i * tau;					
		int ll 				= d_i;						
		int ld 				= twotaupow * d_ip1;					
		int tupleBitLength 	= 1 + ln + ll + ld;
		int w				= 4;
		int bucketSize		= tupleBitLength * w;
		int n				= d_i + 4;
		
		// PostAccess-1 inputs
		String sC_P_p		= Util.addZero(new BigInteger(n*bucketSize, rnd).toString(2), n*bucketSize);
		String sC_T_p		= Util.addZero(new BigInteger(tupleBitLength, rnd).toString(2), tupleBitLength);
		String sE_P_p		= Util.addZero(new BigInteger(n*bucketSize, rnd).toString(2), n*bucketSize);
		String sE_T_p		= Util.addZero(new BigInteger(tupleBitLength, rnd).toString(2), tupleBitLength);
		String Li			= Util.addZero(new BigInteger(ll, rnd).toString(2), ll);		
		
		// protocol
		// step 1
		int[] alpha1_j = new int[w];
		int[] alpha2_j = new int[w];
		for (int j=0; j<d_i; j++) {
			String sC_input1 = "";
			String sE_input1 = "";
			String sC_input2 = "";
			String sE_input2 = "";
			String sC_bucket = sC_P_p.substring(j*bucketSize, (j+1)*bucketSize);
			String sE_bucket = sE_P_p.substring(j*bucketSize, (j+1)*bucketSize);
			for (int l=0; l<w; l++) {
				String sC_tuple = sC_bucket.substring(l*tupleBitLength, (l+1)*tupleBitLength);
				sC_input1 += sC_tuple.substring(0, 1);
				sC_input2 += sC_tuple.substring(1+ln, 1+ln+ll).substring(j+1, j+2);
				String sE_tuple = sE_bucket.substring(l*tupleBitLength, (l+1)*tupleBitLength);
				sE_input1 += sC_tuple.substring(0, 1);
				sE_input2 += (Character.getNumericValue(sE_tuple.substring(1+ln, 1+ln+ll).charAt(j+1))^1^Character.getNumericValue(Li.charAt(j+1)));
			}
			String sC_input = "00" + sC_input2 + sC_input1; // enable + dir + fb
			String sE_input = "00" + sE_input2 + sE_input1;
			String GCFOutput = GCFTestServer.executeGCF(sC_input, sE_input, "F2FT");
			alpha1_j[j] = GCFOutput.substring(2).indexOf('1', 2);
			alpha2_j[j] = GCFOutput.substring(2).indexOf('1', alpha1_j[j]+1);
		}
		
		// step 2
		String sC_fb = "00";
		String sE_fb = "00";
		for (int j=d_i; j<n; j++) {
			String sC_bucket = sC_P_p.substring(j*bucketSize, (j+1)*bucketSize);
			String sE_bucket = sE_P_p.substring(j*bucketSize, (j+1)*bucketSize);
			for (int l=0; l<w; l++) {
				sC_fb += sC_bucket.substring(l*tupleBitLength, l*tupleBitLength+1);
				sE_fb += sE_bucket.substring(l*tupleBitLength, l*tupleBitLength+1);
			}
		}
		String GCFOutput = GCFTestServer.executeGCF(sC_fb, sE_fb, "F2ET");
		int alpha1_d = GCFOutput.substring(2).indexOf('1');
		int alpha2_d = GCFOutput.substring(2).indexOf('1', alpha1_d+1);
		
		// step 3
		int k = w * n;
		Integer[] beta = new Integer[k];
		for (int j=0; j<n; j++)
			for (int l=0; l<w; l++) {
				if (j == 0 && l == alpha1_j[0])
					beta[w*j+l] = k + 1;
				else if (j == 0 && l == alpha2_j[0])
					beta[w*j+l] = k + 2;
				else if (1 <= j && j <= (d_i-1) && l == alpha1_j[j])
					beta[w*j+l] = w * (j-1) + alpha1_j[j-1];
				else if (1 <= j && j <= (d_i-1) && l == alpha2_j[j])
					beta[w*j+l] = w * (j-1) + alpha2_j[j-1];
				else if (j >= d_i && (w*(j-d_i)+l) == alpha1_d)
					beta[w*j+l] = w * (d_i-1) + alpha1_j[d_i-1];
				else if (j >= d_i && (w*(j-d_i)+l) == alpha2_d)
					beta[w*j+l] = w * (d_i-1) + alpha2_j[d_i-1];
				else
					beta[w*j+l] = w * j + l;
			}
		Integer[] I = beta;
		
		// step 4
		String[] sC_a = new String[k+2];
		String[] sE_a = new String[k+2];
		for (int j=0; j<n; j++)
			for (int l=0; l<w; l++) {
				sC_a[w*j+l] = sC_P_p.substring(j*bucketSize, (j+1)*bucketSize).substring(l*tupleBitLength, l*tupleBitLength);
				sE_a[w*j+l] = sE_P_p.substring(j*bucketSize, (j+1)*bucketSize).substring(l*tupleBitLength, l*tupleBitLength);
			}
		sC_a[k] = sC_T_p;
		sC_a[k+1] = Util.addZero(new BigInteger(tupleBitLength-1, rnd).toString(2), tupleBitLength);
		sE_a[k] = sE_T_p;
		sE_a[k+1] = Util.addZero(new BigInteger(tupleBitLength-1, rnd).toString(2), tupleBitLength);
		
		// step 5
		String[][] output = SSOT.executeSSOT(sC_a, sE_a, I);
		
		// outputs
		System.out.println("sC: ");
		Util.printArrV(output[0]);
		System.out.println("sE: ");
		Util.printArrV(output[1]);
		
		// check correctness
		// TODO
	}

}
