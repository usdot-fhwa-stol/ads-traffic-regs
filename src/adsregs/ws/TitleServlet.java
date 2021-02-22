/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCIndex;
import adsregs.bc.Title;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aaron.cherney
 */
public class TitleServlet extends HttpServlet
{
	@Override
	public void doGet(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws ServletException, IOException
	{
		ArrayList<Title> oTitles = new ArrayList();
		BCIndex.getCurrentTitles(oTitles);
		
		StringBuilder sValBuf = new StringBuilder();
		StringBuilder sResBuf = new StringBuilder();
		
		sResBuf.append('{');
		for (Title oTitle : oTitles)
		{
			oTitle.getValue("currid", sValBuf);
			sResBuf.append('\"').append(sValBuf).append("\":\"");
			oTitle.getValue("label", sValBuf);
			sResBuf.append(sValBuf).append("\",");
		}
		sResBuf.setLength(sResBuf.length() - 1); // remove trailing comma
		sResBuf.append('}');
		
		oRes.getWriter().append(sResBuf);
		oRes.setContentType("application/json");
	}
}
