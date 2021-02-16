/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.bc;

import adsregs.util.JsonReader;
import adsregs.util.Text;

/**
 *
 * @author aaron.cherney
 */
public abstract class BCBase implements Comparable<BCBase>
{
	protected final int[][] m_nIndices;
	protected final StringBuilder m_sJson;
	
	
	protected BCBase(JsonReader oReader)
	{
		m_sJson = new StringBuilder(oReader.getJson());
		m_nIndices = new int[oReader.size()][];
		for (int nIndex = 0; nIndex < m_nIndices.length; nIndex++)
		{
			int[] nOriginal = oReader.get(nIndex);
			int[] nTemp = new int[nOriginal.length];
			System.arraycopy(nOriginal, 0, nTemp, 0, nTemp.length);
			m_nIndices[nIndex] = nTemp;
		}
	}	
	
	public String[] getKeys()
	{
		if (m_nIndices == null || m_nIndices.length == 0)
			return null;

		String[] sKeys = new String[m_nIndices.length];
		int nIndex = sKeys.length;
		while (nIndex-- > 0)
		{
			int[] nKeyVal = m_nIndices[nIndex]; // use key start and end
			sKeys[nIndex] = m_sJson.substring(nKeyVal[0], nKeyVal[1]);
		}
		return sKeys;
	}


	public void getValue(CharSequence sKey, StringBuilder sBuf)
	{
		sBuf.setLength(0);
		boolean bFound = false;
		int nIndex = m_nIndices.length;
		while (!bFound && nIndex-- > 0)
			bFound = compare(m_nIndices[nIndex], sKey) == 0;

		if (bFound)
		{
			int[] nKeyVal = m_nIndices[nIndex]; // use value start and end
			sBuf.append(m_sJson, nKeyVal[2], nKeyVal[3]);
		}
	}
	
	
	public String getValue(CharSequence sKey)
	{
		boolean bFound = false;
		int nIndex = m_nIndices.length;
		while (!bFound && nIndex-- > 0)
			bFound = compare(m_nIndices[nIndex], sKey) == 0;

		if (bFound)
		{
			int[] nKeyVal = m_nIndices[nIndex]; // use value start and end
			return m_sJson.substring(nKeyVal[2], nKeyVal[3]);
		}
		
		return null;
	}
	
	
	public void appendJson(StringBuilder sBuf)
	{
		sBuf.append(m_sJson);
	}
	
	
	public int compare(int[] nKeyVal, CharSequence sKey)
	{
		int nLen = nKeyVal[1] - nKeyVal[0];

		if (nLen < sKey.length())
			return -1;

		if (nLen > sKey.length())
			return 1;

		int nCompare = 0;
		int nIndex = 0;
		while (nCompare == 0 && nIndex < nLen)
		{
			nCompare = m_sJson.charAt(nKeyVal[0] + nIndex) - sKey.charAt(nIndex);
			++nIndex;
		}
		return nCompare;
	}


	@Override
	public int compareTo(BCBase o)
	{
		StringBuilder sBuf1 = BCFactory.getIdBuffer();
		StringBuilder sBuf2 = BCFactory.getIdBuffer();
		getValue("currid", sBuf1);
		o.getValue("currid", sBuf2);
		return Text.compare(sBuf1, sBuf2);
	}
}
