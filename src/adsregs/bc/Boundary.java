/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.bc;

import adsregs.util.JsonReader;

/**
 *
 * @author aaron.cherney
 */
public class Boundary extends BCBase
{
	public double[] m_dLons;
	public double[] m_dLats;
	public double[] m_dBBox;
	
	
	public Boundary(JsonReader oJson)
	   throws Exception
	{
		super(oJson);
		String[] sLons = getValue("lon").split(",");
		m_dLons = new double[sLons.length];
		String[] sLats = getValue("lat").split(",");
		m_dLats = new double[sLats.length];
		if (sLons.length != sLats.length)
			throw new Exception("Invalid Boundary file: lon and lat arrays are not the same size.");
		
		for (int nIndex = 0; nIndex < sLons.length; nIndex++)
		{
			m_dLons[nIndex] = Double.parseDouble(sLons[nIndex]);
			m_dLats[nIndex] = Double.parseDouble(sLats[nIndex]);
		}
		
		m_dBBox = new double[]{Double.parseDouble(getValue("minlon")), Double.parseDouble(getValue("minlat")), Double.parseDouble(getValue("maxlon")), Double.parseDouble(getValue("maxlat"))};
	}
}
