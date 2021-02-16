package adsregs.util;

import adsregs.bc.BCBase;
import adsregs.bc.BCFactory;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;


public class JsonReader extends ArrayList<int[]>
{
	private static enum STATE
	{
		BEFORE_ROOT, 
		BEFORE_KEY, 
		START_KEY, 
		END_KEY, 
		BEFORE_VALUE, 
		BEFORE_ARRAY_ELEMENT, 
		START_NULL, 
		START_FALSE, 
		START_TRUE, 
		START_STRING, 
		START_NUMBER, 
		END_ARRAY_ELEMENT, 
		END_VALUE
	};

	protected final StringBuilder m_sJson = new StringBuilder();


	public JsonReader()
	{
	}


	public void load(Reader oJson)
		throws Exception
	{
		int[] nKeyVal = null;
		STATE nCurrState = STATE.BEFORE_ROOT;
		STATE nPrevState = nCurrState;
		m_sJson.setLength(0);

		int nChar;
		while ((nChar = oJson.read()) >= 0)
		{
			if ("\n\t\r".indexOf(nChar) >= 0) // ordered by likely occurrence
				continue; // ignore newline, tab, return chars

			switch (nCurrState)
			{
				case BEFORE_ROOT:
				{
					if (nChar != ' ' && nChar != '{') // only remaining possible whitespace char
						throw new Exception();

					nCurrState = STATE.BEFORE_KEY;
					m_sJson.append((char)nChar);
				}
				break;

				case BEFORE_KEY:
				{
					if (nChar != ' ')
					{
						m_sJson.append((char)nChar);
						switch (nChar)
						{
							case '\"':
							{
								nKeyVal = new int[4]; // initialize start end indices
								nKeyVal[0] = m_sJson.length(); // save key start index
								nCurrState = STATE.START_KEY;
							}
							break;

							case '}': nCurrState = STATE.BEFORE_ROOT; break;
							default: throw new Exception();
						}
					}
				}
				break;

				case START_KEY:
				{
					m_sJson.append((char)nChar); // check for end key condition
					if (nChar == '\"' && m_sJson.charAt(m_sJson.length() - 2) != '\\')
					{
						nKeyVal[1] = m_sJson.length() - 1; // save key end index
						nCurrState = STATE.END_KEY;
					}
				}
				break;

				case END_KEY:
				{
					if (nChar != ' ')
					{
						if (nChar == ':')
						{
							m_sJson.append((char)nChar); // keep separator
							nCurrState = STATE.BEFORE_VALUE;
						}
						else
							throw new Exception();
					}
				}
				break;

				case BEFORE_VALUE: // object values not allowed
				{
					if (nChar != ' ')
					{
						if ("\"[1234567890-nft".indexOf(nChar) < 0)
							throw new Exception();
							
						nPrevState = nCurrState;
						nKeyVal[2] = m_sJson.length(); // save value start index
						m_sJson.append((char)nChar); // append after saving index
						switch (nChar)
						{
							case '\"':
							{
								nKeyVal[2]++; // adjust value start index
								nCurrState = STATE.START_STRING;
							}
							break;

							case '[':
							{
								nKeyVal[2]++; // adjust value start index
								nCurrState = STATE.BEFORE_ARRAY_ELEMENT;
							}
							break;

							case 'n': nCurrState = STATE.START_NULL; break;
							case 'f': nCurrState = STATE.START_FALSE; break;
							case 't': nCurrState = STATE.START_TRUE; break;
							default: nCurrState = STATE.START_NUMBER;
						}
					}
				}
				break;

				case BEFORE_ARRAY_ELEMENT: // child objects and arrays not allowed
				{
					if (nChar != ' ')
					{
						if ("\"1234567890-nft]".indexOf(nChar) < 0)
							throw new Exception();
							
						nPrevState = nCurrState;
						m_sJson.append((char)nChar);
						switch (nChar)
						{
							case '\"': nCurrState = STATE.START_STRING; break;
							case '[': nCurrState = STATE.BEFORE_ARRAY_ELEMENT; break;
							case 'n': nCurrState = STATE.START_NULL; break;
							case 'f': nCurrState = STATE.START_FALSE; break;
							case 't': nCurrState = STATE.START_TRUE; break;
							case ']': nCurrState = STATE.END_VALUE; break;
							default: nCurrState = STATE.START_NUMBER;
						}
					}
				}
				break;

				case START_STRING:
				{
					m_sJson.append((char)nChar); // keeps spaces
					if (nChar == '\"' && m_sJson.charAt(m_sJson.length() - 2) != '\\')
					{
						if (nPrevState == STATE.BEFORE_VALUE) // check for inside array
							nCurrState = STATE.END_VALUE;
						else
							nCurrState = STATE.END_ARRAY_ELEMENT;
					}
				}
				break;

				case START_NULL: // no check for repeated chars
				case START_FALSE:
				case START_TRUE:
				case START_NUMBER: // no check for exponents or repeated sign/decimal chars
				{
					switch (nCurrState) // filters for disallowed chars
					{
						case START_NULL: if (" ,ul]}".indexOf(nChar) < 0) throw new Exception(); break;
						case START_FALSE: if (" ,alse]}".indexOf(nChar) < 0) throw new Exception(); break;
						case START_TRUE: if (" ,rue]}".indexOf(nChar) < 0) throw new Exception(); break;
						default: if (" ,.1234567890-]}".indexOf(nChar) < 0) throw new Exception();
					}

					if (nChar == ' ') // don't save whitespace
					{
						if (nPrevState == STATE.BEFORE_VALUE) // check for inside array
							nCurrState = STATE.END_VALUE;
						else
							nCurrState = STATE.END_ARRAY_ELEMENT;
					}
					else
					{
						m_sJson.append((char)nChar); // keep trigger char
						switch (nChar)
						{
							case ']': nCurrState = STATE.END_VALUE; break;
							case '}':
							{
								nKeyVal[3] = m_sJson.length() - 1;
								add(nKeyVal);
								nKeyVal = null;
								nCurrState = STATE.BEFORE_ROOT;
							}
							break;

							case ',': 
							{
								if (nPrevState == STATE.BEFORE_VALUE) // check for inside array
								{
									nKeyVal[3] = m_sJson.length() - 1;
									add(nKeyVal);
									nKeyVal = null;
									nCurrState = STATE.BEFORE_KEY;
								}
								else
									nCurrState = STATE.BEFORE_ARRAY_ELEMENT;
							}
						}
					}
				}
				break;

				case END_ARRAY_ELEMENT:
				{
					if (" ,]".indexOf(nChar) < 0)
						throw new Exception();

					if (nChar != ' ')
					{
						m_sJson.append((char)nChar);
						if (nChar == ',')
							nCurrState = STATE.BEFORE_ARRAY_ELEMENT;
						else if (nChar == ']')
							nCurrState = STATE.END_VALUE;
					}
				}
				break;

				case END_VALUE:
				{
					if (" ,}".indexOf(nChar) < 0)
						throw new Exception();

					if (nChar != ' ')
					{
						nKeyVal[3] = m_sJson.length(); // save value end index
						m_sJson.append((char)nChar); // save index before appending
						char cTest = m_sJson.charAt(m_sJson.length() - 2);
						if (cTest == '\"' || cTest == ']') // handle string and array values
							nKeyVal[3]--;

						add(nKeyVal); // save key value indices
						nKeyVal = null;

						if (nChar == ',')
							nCurrState = STATE.BEFORE_KEY;
						else if (nChar == '}')
							nCurrState = STATE.BEFORE_ROOT;
					}
				}
			}
		}
	}


	public int compare(int[] nKeyVal, CharSequence sKey)
	{
		int nLen = nKeyVal[1] - nKeyVal[0];

		if (nLen < sKey.length())
			return -1;

		if (nLen > sKey.length())
			return 1;

		int nCompare = 0;
		int nIndex = 0;
		while (nCompare == 0 && nIndex < nLen)
		{
			nCompare = m_sJson.charAt(nKeyVal[0] + nIndex) - sKey.charAt(nIndex);
			++nIndex;
		}
		return nCompare;
	}


	public String[] getKeys()
	{
		if (isEmpty())
			return null;

		String[] sKeys = new String[size()];
		int nIndex = sKeys.length;
		while (nIndex-- > 0)
		{
			int[] nKeyVal = get(nIndex); // use key start and end
			sKeys[nIndex] = m_sJson.substring(nKeyVal[0], nKeyVal[1]);
		}
		return sKeys;
	}


	public void getValue(CharSequence sKey, StringBuilder sBuf)
	{
		sBuf.setLength(0);
		boolean bFound = false;
		int nIndex = size();
		while (!bFound && nIndex-- > 0)
			bFound = compare(get(nIndex), sKey) == 0;

		if (bFound)
		{
			int[] nKeyVal = get(nIndex); // use value start and end
			sBuf.append(m_sJson, nKeyVal[2], nKeyVal[3]);
		}
	}
	
	
	public String getValue(CharSequence sKey)
	{
		boolean bFound = false;
		int nIndex = size();
		while (!bFound && nIndex-- > 0)
			bFound = compare(get(nIndex), sKey) == 0;

		if (bFound)
		{
			int[] nKeyVal = get(nIndex); // use value start and end
			return m_sJson.substring(nKeyVal[2], nKeyVal[3]);
		}
		
		return null;
	}
	
	
	public CharSequence getJson()
	{
		return m_sJson;
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		BCBase oBoundary = BCFactory.createFromJson(Paths.get("C:/Program Files/Apache Software Foundation/Tomcat 9.0/webapps/adsregs/blockroot/koqhSJjsLxQ/V10tPtbZQEk/UfQ0lyqJfEY/koqhSJjsLxQV10tPtbZQEkUfQ0lyqJfEYHXSqmAGNAw.json"));
		BCBase oJurisdiction = BCFactory.createFromJson(Paths.get("C:/Users/aaron.cherney/Documents/ADSRegs/blockroot/99hTAG9gKUb/6Zv0Fx0z525/R7ctRxqiKft/99hTAG9gKUb6Zv0Fx0z525R7ctRxqiKft1gymSL639A.json"));
		System.out.print(Arrays.toString(oBoundary.getKeys()));
		System.out.println();
		System.out.println(oBoundary.getValue("specid"));
		
		System.out.print(Arrays.toString(oJurisdiction.getKeys()));
		System.out.println();
		System.out.println(oJurisdiction.getValue("specid"));
	}
}
