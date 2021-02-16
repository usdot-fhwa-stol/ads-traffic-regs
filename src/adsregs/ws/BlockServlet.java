/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCBase;
import adsregs.bc.BCIndex;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aaron.cherney
 */
public class BlockServlet extends HttpServlet
{
	@Override
	public void doPost(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws ServletException, IOException
	{
		String sId = oReq.getParameter("id");
		BCBase oBlock = BCIndex.getBlockById(sId);
		StringBuilder sBuf = new StringBuilder();
		if (oBlock == null)
			sBuf.append("{}");
		else
			oBlock.appendJson(sBuf);
		
		oRes.setContentType("application/json");
		oRes.getWriter().append(sBuf);
	}
}
