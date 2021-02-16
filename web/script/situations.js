/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

function initialize()
{
	$('#instructDialog').dialog(
			{width: 800,
//			 height: 480,
			 autoOpen: false,
			 create: createDialog,
			 open: openDialog,
			 close: closeDialog,
			 resizeStop: afterResize
			});
	$('.ui-dialog-titlebar').addClass('w3-sw-tarnished-trumpet');
	$('td span').each(function() 
		{
			if ($(this).attr('id'))
				$(this).click({'id': $(this).attr('id'), 'refs': $(this).attr('refs')}, regClick);
		});
}

function regClick(obj)
{
	$.ajax('api/block',
	{
		dataType: 'json',
		method: 'POST',
		data: {'id' : obj.data.id},
		refs: obj.data.refs
	}).done(blockSuccess);
}


function blockSuccess(oData, sStatus, oJqXHR)
{
	let oDialog = $('#instructDialog');
	let sTitleBar;
	let sHtml;
	if (oData.content)
	{
		sTitleBar = oData.label;
		sHtml = '<pre style="white-space: pre-wrap;">\n' + oData.content + '\n</pre>';
	}
	else
	{
		sTitleBar = 'Error';
		sHtml = 'Error loading instruction';
	}
	
	oDialog.siblings().find(".ui-dialog-title").html(sTitleBar);
	oDialog.html(sHtml);
	if (this.refs)
	{
		let aRefs = this.refs.split(',');
		for (let sTag of aRefs.values())
			oDialog.children().find("#" + sTag).attr('style', 'background-color: #ff0;');
		oDialog.dialog('open');
		oDialog.children().find('span', '#' + aRefs[0]).get(0).scrollIntoView();
	}
	else
	{
		oDialog.children().find("span").attr('style', 'background-color: #ff0;');
		oDialog.dialog('open');
	}
	
}

function matrixSuccess(oData, sStatus, oJqXHR)
{
	let aRows = oData.rows;
	aRows.sort();
	let sTable = '<table class="centered border-collapse"><tr><th class="w3-sw-tarnished-trumpet"></th>';
	let nCols = oData.cols.length;
	for (let sCol of oData.cols.values())
		sTable += '<th class="w3-sw-tarnished-trumpet">' + sCol + '</th>';
	
	sTable += '</tr>';
	for (let sRow of aRows.values())
	{
		sTable += '<tr>';
		sTable += '<th class="w3-sw-tarnished-trumpet">' + sRow + '</th>';
		let nIndices = oData[sRow];
		for (let nIndex = 0; nIndex < nCols; nIndex++)
		{
			sTable += '<td class="w3-center">';
			if (binarySearch(nIndices, nIndex, intCmp) >= 0)
				sTable += '<i class="fas fa-check"></i>';
			sTable += '</td>';
		}
		sTable += '</tr>';
	}
	$('#report-output').html(sTable);
}

function binarySearch(aArr, oKey, fnComp) 
{
	let nLow = 0;
	let nHigh = aArr.length - 1;

	while (nLow <= nHigh) 
	{
		let nMid = (nLow + nHigh) >> 1;
		let oMidVal = aArr[nMid];
		let nCmp = fnComp(oMidVal, oKey);

		if (nCmp < 0)
			nLow = nMid + 1;
		else if (nCmp > 0)
			nHigh = nMid - 1;
		else
			return nMid; // key found
	}
	return -(nLow + 1);  // key not found
}

function createDialog(event, ui)
{
	$(this).parent().css({'max-width': 'calc(80% - 50px', 'max-height': 'calc(80% - 50px'});
}

function openDialog(event, ui)
{
	$(this).css('max-height', parseFloat($(this).parent().css('height')) * 0.9);
}

function afterResize(event, ui)
{
	$(this).css('max-height', parseFloat($(this).parent().css('height')) * 0.9);
}

function closeDialog(event, ui)
{
	$('#instructList li').removeClass('w3-sw-kale-green');
}

function intCmp(n1, n2)
{
	return n1 - n2;
}

$(document).ready(initialize);

