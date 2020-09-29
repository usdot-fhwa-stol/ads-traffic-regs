/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCIndex;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 *
 * @author aaron.cherney
 */
public class InitSystem extends HttpServlet
{
	@Override
	public void init(ServletConfig oConfig)
	   throws ServletException
	{
		try
		{
			BCIndex.createIndex(oConfig.getInitParameter("bcindexfile"), oConfig.getInitParameter("bcbasedir"));
//			BCIndex.updateTail("ak0-w6pVJxc2cuTL3W8lO4glnLSYW5v4-2mPGasu19n", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", System.currentTimeMillis());
		}
		catch (Exception oEx)
		{
			throw new ServletException(oEx);
		}
	}
}
