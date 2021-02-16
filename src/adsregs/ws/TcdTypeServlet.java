/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCBase;
import adsregs.bc.BCBaseList;
import adsregs.bc.BCIndex;
import adsregs.util.Text;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aaron.cherney
 */
public class TcdTypeServlet extends HttpServlet
{
	@Override
	public void doPost(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws ServletException, IOException
	{
		String[] sJurs = oReq.getParameterValues("id");
		Arrays.sort(sJurs);
		char[][] cJurIds = new char[sJurs.length][];
		for (int nIndex = 0; nIndex < cJurIds.length; nIndex++)
			cJurIds[nIndex] = Text.getCharArray(sJurs[nIndex]);
		ArrayList<BCBaseList> oTcdTypeList = new ArrayList();
		BCIndex.getTcdTypesByJur(oTcdTypeList, cJurIds);
		oRes.setContentType("application/json");
		
		StringBuilder sBuf = new StringBuilder();
		sBuf.append('[');
		if (oTcdTypeList.isEmpty())
		{
			sBuf.append(']');
			oRes.getWriter().append(sBuf);
			return;
		}
		
		for (BCBase oTcdType : oTcdTypeList.get(0))
		{
			oTcdType.appendJson(sBuf);
			sBuf.append(',');
		}
		
		sBuf.setLength(sBuf.length() - 1);
		sBuf.append(']');
		
		oRes.getWriter().append(sBuf);
	}
}
