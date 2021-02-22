/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

/**
 *
 * @author aaron.cherney
 */
public class CenPlace
{
	public String m_sName;
	public int m_nGeoId;
	public int m_nStateFp;
	public String m_sLabel;

	public CenPlace(String sLabel)
	{
		m_sLabel = sLabel;
	}
	public CenPlace(String sName, int nGeoId, int nStateFp, String[] sAbbrevs)
	{
		m_sName = sName;
		m_nGeoId = nGeoId;
		m_nStateFp = nStateFp;
		if (nGeoId > sAbbrevs.length)
			m_sLabel = m_sName + ", " + sAbbrevs[m_nStateFp];
		else
			m_sLabel = m_sName;
	}

	@Override
	public String toString()
	{
		return m_sName + " " + m_nStateFp + " " + m_nGeoId;
	}
}
