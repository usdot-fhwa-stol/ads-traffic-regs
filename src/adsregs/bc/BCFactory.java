/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.bc;

import adsregs.util.JsonReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 *
 * @author aaron.cherney
 */
public abstract class BCFactory
{
	public static final int JURISDICTION = 0;
	public static final int TITLE = 1;
	public static final int INSTRUCTION = 2;
	public static final int SITUATION = 3;
	public static final int TCDTYPE = 4;
	public static final int BOUNDARY = 5;
	public static final String[] CURSPECS;
	private static final HashMap<String, String> SPECS = new HashMap();
	static
	{
		SPECS.put("eFQYRLXjJNehfyAcl0raXWYv5Q2JrbLTr9pW4kdG5eg", "adsregs.bc.Boundary");
		SPECS.put("XvjubB-LL7-JKfTUI1NX2pbA9xHGm1Xir9eATiSD8Ss", "adsregs.bc.Jurisdiction");
		SPECS.put("T8kx8J_0UgNmL5oTP1Edj5JqwlV8uf_g4doTR9JJhV8", "adsregs.bc.Situation");
		SPECS.put("KH7-vrSYgPGnAfPlPKER-c+0k_9jVFCgNJ2hrvS0SX8", "adsregs.bc.Title");
		SPECS.put("pAyju5PfE-pdmnlIQRR2VW0U03eosLNwgrQDAzXV2zA", "adsregs.bc.Instruction");
		SPECS.put("b10WN1VecV8vsHpCfyQA1jkA40ITFxDp53cq4zjrf14", "adsregs.bc.TcdType");
		
		CURSPECS = new String[6];
		CURSPECS[JURISDICTION] = "XvjubB-LL7-JKfTUI1NX2pbA9xHGm1Xir9eATiSD8Ss";
		CURSPECS[TITLE] = "KH7-vrSYgPGnAfPlPKER-c+0k_9jVFCgNJ2hrvS0SX8";
		CURSPECS[INSTRUCTION] = "pAyju5PfE-pdmnlIQRR2VW0U03eosLNwgrQDAzXV2zA";
		CURSPECS[SITUATION] = "T8kx8J_0UgNmL5oTP1Edj5JqwlV8uf_g4doTR9JJhV8";
		CURSPECS[TCDTYPE] = "b10WN1VecV8vsHpCfyQA1jkA40ITFxDp53cq4zjrf14";
		CURSPECS[BOUNDARY] = "eFQYRLXjJNehfyAcl0raXWYv5Q2JrbLTr9pW4kdG5eg";
	}
	
	
	public static BCBase createFromJson(Path oJsonFile)
	   throws Exception
	{
		try (BufferedReader oJson = Files.newBufferedReader(oJsonFile, StandardCharsets.UTF_8))
		{
			JsonReader oJsonReader = new JsonReader();
			oJsonReader.load(oJson);
			StringBuilder sBuf = new StringBuilder();
			oJsonReader.getValue("specid", sBuf);
			return (BCBase)Class.forName(SPECS.get(sBuf.toString())).getConstructor(JsonReader.class).newInstance(oJsonReader);
		}
		catch (Exception oEx)
		{
			throw new Exception(String.format("Error loading file: %s\nException message: %s", oJsonFile.toString(), oEx.getMessage()));
		}
	}
	
	
	public static String getClass(String sSpec)
	{
		return SPECS.get(sSpec);
	}
	
	
	public static StringBuilder getIdBuffer()
	{
		return new StringBuilder(43);
	}
}
