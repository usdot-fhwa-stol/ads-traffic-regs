/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.util;

/**
 *
 * @author aaron.cherney
 */
public abstract class Geo
{
	public static boolean isInBoundingBox(double dX, double dY, double[] dBBox)
	{
		return dX >= dBBox[0] && dX <= dBBox[2] && dY >= dBBox[1] && dY <= dBBox[3];
	}
}
