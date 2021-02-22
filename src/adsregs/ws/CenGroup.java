/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import java.util.ArrayList;

/**
 *
 * @author aaron.cherney
 */
public class CenGroup extends ArrayList<CenPlace> implements Comparable<char[]>
{
	char[] m_cKey;

	public CenGroup(char[] cKey)
	{
		m_cKey = new char[cKey.length];
		System.arraycopy(cKey, 0, m_cKey, 0, cKey.length);
	}


	@Override
	public int compareTo(char[] oRhs)
	{
		int nIndex = 0;
		int nComp = 0;
		while (nIndex < m_cKey.length)
		{
			nComp = m_cKey[nIndex] - oRhs[nIndex];
			if (nComp != 0)
				break;
			++nIndex;
		}

		return nComp;
	}
}
