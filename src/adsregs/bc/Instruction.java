/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.bc;

import adsregs.util.JsonReader;
import adsregs.util.Text;
import java.util.Arrays;

/**
 *
 * @author aaron.cherney
 */
public class Instruction extends BCBase
{
	public char[][] m_cSituations;
	public Instruction(JsonReader oReader)
	{
		super(oReader);
		String sSituations = getValue("situations");
		if (sSituations.isEmpty())
		{
			m_cSituations = new char[0][];
			return;
		}
		String[] sArr = sSituations.replaceAll("\"", "").split(",");
		m_cSituations = new char[sArr.length][];
		for (int nIndex = 0; nIndex < sArr.length; nIndex++)
			m_cSituations[nIndex] = Text.getCharArray(sArr[nIndex]);
		
		Arrays.sort(m_cSituations, Text.CHAR_ARRAY_COMP);
	}
}
