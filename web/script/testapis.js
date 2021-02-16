/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

function initialize()
{
	$('#btnJurisdictionSubmit').click(function() 
		{
			$.ajax('api/jurisdictions', 
			{
				dataType: 'json',
				method: 'POST',
				data: $('#jurisdictionForm').serialize()
			}).done(jurisdictionSuccess);
		});
	
	$('#btnBoundarySubmit').click(function()
		{
			$.ajax('api/boundaries',
			{
				dataType: 'json',
				method: 'POST',
				data: $('#boundaryForm').serialize()
			}).done(boundarySuccess);
		});
		
	$('#btnSituationSubmit').click(function()
		{
			$.ajax('api/situations',
			{
				dataType: 'json',
				method: 'POST',
				data: $('#situationForm').serialize()
			}).done(situationSuccess);
		});
}

function jurisdictionSuccess(oData, sStatus, oJqXHR)
{
	$('#jurisdictionOutput').html('<pre>' + JSON.stringify(oData, null, '\t') +'</pre>');
	for (let oForm of [$('#boundaries'), $('#situations')].values())
	{
		oForm.find('option').remove().end();
		for (let sKey of Object.keys(oData))
		{
			oForm.append($('<option>').attr('value', sKey).text(sKey));
		}
	}
}

function boundarySuccess(oData, sStatus, oJqXHR)
{
	$('#boundaryOutput').html('<pre>' + JSON.stringify(oData, null, '\t') +'</pre>');
}

function situationSuccess(oData, sStatus, oJqXHR)
{
	$('#situationOutput').html('<pre>' + JSON.stringify(oData, null, '\t') +'</pre>');
}

function matrixSuccess(oData, sStatus, oJqXHR)
{
	let aRows = oData.rows;
	aRows.sort();
	let sTable = '<table><tr><th></th>';
	let nCols = oData.cols.length;
	for (let sCol of oData.cols.values())
		sTable += '<th>' + sCol + '</th>';
	
	sTable += '</tr>';
	for (let sRow of aRows.values())
	{
		sTable += '<tr>';
		sTable += '<th>' + sRow + '</th>';
		let nIndices = oData[sRow];
		for (let nIndex = 0; nIndex < nCols; nIndex++)
		{
			sTable += '<td class="w3-center"><i class="fas ';
			if (binarySearch(nIndices, nIndex, intCmp) >= 0)
				sTable += 'fa-check-circle" style="color:#0c0;"';
			else
				sTable += 'fa-ban" style="color:#c00;"';
			sTable += '></i></td>';
		}
		sTable += '</tr>';
	}
	$('#matrixOutput').html(sTable);
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


function intCmp(n1, n2)
{
	return n1 - n2;
}


$(document).ready(initialize);

