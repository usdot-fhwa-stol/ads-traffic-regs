/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.bc;

import adsregs.util.JsonReader;
import java.io.IOException;

/**
 *
 * @author aaron.cherney
 */
public class Jurisdiction extends BCBase
{
	public Jurisdiction(JsonReader oJson)
	   throws IOException
	{
		super(oJson);
	}
}
