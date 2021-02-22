/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var g_lLastInput = 0;
var g_lTypingTimeout = 1000;

function initialize()
{
	$('#textlookup').keyup(textLookupKeyup);
}

function textLookupKeyup()
{
	let sText = $('#textlookup').val();
	g_lLastInput = new Date().getTime();
	if (sText.length < 3)
		return;
	
	setTimeout(sendQuery, g_lTypingTimeout);
}

function sendQuery()
{
	let sText = $('#textlookup').val();
	if (sText.length < 3)
		return;
	let lGap = new Date().getTime() - g_lLastInput;
	console.log(lGap);
	if (lGap >= g_lTypingTimeout)
	{
		$.ajax("api/placelookup/" + sText,
		{
			'dataType': 'json',
			'method': 'GET'
		}).done(lookupSuccess);
	}
}


function lookupSuccess(data, textStatus, jqXHR)
{
	let oList = $('#placeResults');
	oList.empty();
	for (let sPlace of data.values())
		oList.append('<li>' + sPlace + '</li>');
}

$(document).ready(initialize);