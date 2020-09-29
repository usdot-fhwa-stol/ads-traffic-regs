/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.bc;

import adsregs.util.Text;
import java.util.ArrayList;

/**
 *
 * @author aaron.cherney
 */
public class BCBaseList extends ArrayList<BCBase> implements Comparable<BCBaseList>
{
	public char[] m_cId;
	public String m_sLabel;
	public ArrayList<char[]> m_oIds = new ArrayList();

	public BCBaseList()
	{
		m_cId = new char[43];
	}
	
	
	public BCBaseList(char[] cId, String sLabel)
	{
		m_cId = cId;
		m_sLabel = sLabel;
	}
	
	
	@Override
	public int compareTo(BCBaseList o)
	{
		return Text.compare(m_cId, o.m_cId);
	}
}
