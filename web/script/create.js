/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

let g_bUpdateJurisdiction = true;
let g_bUpdateSituation = true;
let g_bUpdateTitle = true;
let g_bUpdateInstruction = true;
let oMap;

function initialize()
{
	oMap = new mapboxgl.Map({container: 'map-container', style: 'mapbox/light-v9.json', attributionControl: false, minZoom:4, maxZoom: 24, center: [-98.585522, 39.8333333], zoom: 4, accessToken: 'pk.eyJ1Ijoia3J1ZWdlcmIiLCJhIjoiY2l6ZDl4dTlwMjJvaDJ3bW44bXFkd2NrOSJ9.KXqbeWgASgEUYQu0oi7Hbg'});
	oMap.dragRotate.disable();
	oMap.touchZoomRotate.disableRotation();
	oMap.addControl(new mapboxgl.NavigationControl({showCompass: false}));
	oMap.on('load', function()
	{
		oMap.addSource('place-poly', {'type': 'geojson', 'data': {'type': 'FeatureCollection', 'features': []}});
		oMap.addLayer({'id': 'place-poly', 'type': 'line', 'source': 'place-poly', 'paint':{'line-opacity': 0.6, 'line-color': 'black', 'line-width': 5}});
	});
	$('#type-accordion').accordion({'collapsible': true, 'activate': activateAcc, 'active': false});
	let oJurCache = {};
	$('#jurisdiction-label').autocomplete(
	{
		minLength: 3,
		source: function(oReq, oResponse)
		{
			let sTerm = oReq.term;
			if (sTerm in oJurCache)
			{
				oResponse(oJurCache[sTerm]);
				return;
			}
			$.ajax(
			{
				'url': 'api/placelookup',
				'method': 'POST',
				'dataType': 'json',
				'data': {'lookup': sTerm}
			}).done(function(oData, sStatus, oXhr) 
			{
				oJurCache[sTerm] = oData;
				oResponse(oData);
			}).fail(function()
			{
				alert('Jurisdiction request failed');
				oResponse([]);
			});
		},
		select: function(oEvent, oUi)
		{
			$.ajax(
			{
				'url': 'api/placelookup/geo',
				'method': 'POST',
				'dataType': 'json',
				'data': {'place': oUi.item.value}
			}).done(function(oPolygon, sStatus, oXhr)
			{
				let dMinX = Number.MAX_VALUE;
				let dMaxX = -Number.MAX_VALUE;
				let dMinY = Number.MAX_VALUE;
				let dMaxY = -Number.MAX_VALUE;
//				let aNewRings = [];
				let oSrc = oMap.getSource('place-poly');
				let oData = oSrc._data;
				oData.features = [];
				for (let nRingIndex = 0; nRingIndex < oPolygon.length; nRingIndex++)
				{
					let aRing = oPolygon[nRingIndex];
					let aNewRing = [];
					aNewRing.push([fromIntDeg(aRing[0][0]), fromIntDeg(aRing[0][1])]);
					for (let nIndex = 1; nIndex < aRing.length; nIndex++)
					{
						aRing[nIndex][0] += aRing[nIndex - 1][0];
						aRing[nIndex][1] += aRing[nIndex - 1][1];
						let dX = fromIntDeg(aRing[nIndex][0]);
						let dY = fromIntDeg(aRing[nIndex][1]);
						aNewRing.push([dX, dY]);
						if (dX > dMaxX)
							dMaxX = dX;
						if (dX < dMinX)
							dMinX = dX;
						if (dY > dMaxY)
							dMaxY = dY;
						if (dY < dMinY)
							dMinY = dY;
					}
					if (aNewRing[0][0] !== aNewRing[aNewRing.length - 1][0] || aNewRing[0][1] !== aNewRing[aNewRing.length - 1][1]) // first and last coordinate need to be the same
						aNewRing.push([aNewRing[0][0], aNewRing[0][1]]);
//					aNewRings.push(aNewRing);
					oData.features.push({'type': 'Feature', 'geometry': {'type': 'LineString', 'coordinates': aNewRing}});
				}
				$('#map-container').show();
				oMap.resize();
				
				
				oSrc.setData(oData);
				oMap.fitBounds([[dMinX, dMinY], [dMaxX, dMaxY]], {'padding': 50});
			}).fail(function() 
			{
				alert('Failed to load jurisdiction geometry');
			});
		}
	});
	
	let oJurs = {};
	let aJurNames = [];
	let oJurisdictionAC = 
	{
		'minLength': 0,
		'source': function(oReq, oResponse)
		{
			if (g_bUpdateJurisdiction)
			{
				$.ajax(
				{
					'url': 'api/jurisdictions',
					'method': 'GET',
					'dataType': 'json'
				}).done(function(oData, sStatus, oXhr)
				{
					g_bUpdateJurisdiction = false;
					oJurs = oData;
					aJurNames = Object.values(oData); // response is {'jurid1':'jurname1','jurid2':'jurname2'...}
					aJurNames.sort();
					oResponse(aJurNames);
				}).fail(function()
				{
					g_bUpdateJurisdiction = true;
					oResponse([]);
				});
			}
			else
			{
				oResponse(aJurNames);
			}
		},
		'response': response
	};
	$('#title-jurisdiction').autocomplete(oJurisdictionAC);
	$('#title-jurisdiction').on('autocompleteselect', function(oEvent, oUi) 
	{
		let sJur = oUi.item.value;
		for (let [id, label] of Object.entries(oJurs))
		{
			if (sJur === label)
			{
				$('#title-jurisdiction-id').val(id);
				break;
			}
		}
	});
	$('#tcd-jurisdiction').autocomplete(oJurisdictionAC);
	$('#tcd-jurisdiction').on('autocompleteselect', function(oEvent, oUi) 
	{
		$('#tcd-instructions').prop('disabled', false);
		let sJur = oUi.item.value;
		for (let [id, label] of Object.entries(oJurs))
		{
			if (sJur === label)
			{
				$('#tcd-jurisdiction-id').val(id);
				break;
			}
		}
	});
	$('#tcd-jurisdiction').on('autocompleteopen', function(oEvent, oUi)
	{
		$('#tcd-instructions').prop('disabled', true);
		$('#tcd-selected-instructions option').each(function() {$(this).remove();});
		g_bUpdateInstruction = true;
	});
	
	let aTitleNames = [];
	let oTitles = {};
	$('#instruction-title').autocomplete(
	{
		'minLength': 0,
		'source': function(oReq, oResponse)
		{
			if (g_bUpdateTitle)
			{
				$.ajax(
				{
					'url': 'api/titles',
					'method': 'GET',
					'dataType': 'json'
				}).done(function(oData, sStatus, oXhr)
				{
					g_bUpdateTitle = false;
					oTitles = oData;
					aTitleNames = Object.values(oData); 
					aTitleNames.sort();
					oResponse(aTitleNames);
				}).fail(function()
				{
					g_bUpdateTitle = true;
					oResponse([]);
				});
			}
			else
			{
				oResponse(aTitleNames);
			}
		},
		'response': response,
		'select' : function (oEvent, oUi) 
		{
			let sId = '';
			let sTitle = oUi.item.value;
			for (let [id, label] of Object.entries(oTitles))
			{
				if (sTitle === label)
				{
					$('#instruction-title-id').val(id);
					break;
				}
			}
		}
	});
	let aInstructionNames = [];
	$('#tcd-instructions').autocomplete(
	{
		'minLength': 0,
		'source': function(oReq, oResponse)
		{
			if (g_bUpdateInstruction)
			{
				let sId = '';
				let sJur = $('#tcd-jurisdiction').val();
				for (let [id, label] of Object.entries(oJurs))
				{
					if (sJur === label)
					{
						sId = id;
						break;
					}
				}
				
				$.ajax(
				{
					'url': 'api/instructions',
					'method': 'POST',
					'dataType': 'json',
					'data': {'id': sId}
				}).done(function(oData, sStatus, oXhr)
				{
					aInstructionNames = [];
					
					for (let [sKey, oValue] of Object.entries(oData))
					{
						for (let oInstruction of oValue.instructions.values())
							aInstructionNames.push(oInstruction.label);
					}
					aInstructionNames.sort();
					oResponse(aInstructionNames);
					g_bUpdateInstruction = false;
				}).fail(function()
				{
					g_bUpdateInstruction = true;
					oResponse([]);
				});
			}
			else
			{
				oResponse(aInstructionNames);
			}
		},
		'response': responseAny,
		'select': function(oEvent, oUi)
		{
			let bAdd = true;
			$('#tcd-selected-instructions option').each(function() {if ($(this).val() === oUi.item.value) bAdd = false;});
			if (bAdd)
			{
				$('#tcd-selected-instructions').append(new Option(oUi.item.label, oUi.item.label));
				sortSelectOpts('#tcd-selected-instructions');
				$('#tcd-selected-instructions option').each(function() 
				{
					$(this).on('dblclick', function() 
					{
						$(this).remove();
					});
				});
			}
			$('#tcd-instructions').val('');
			oEvent.preventDefault();
		}
	});
	
	
	$('#section-btn').click(function(oEvent)
	{
		oEvent.preventDefault();
		$('#instruction-content').val($('#instruction-content').val() + '§');
	});
	$('#tab-btn').click(function(oEvent)
	{
		oEvent.preventDefault();
		$('#instruction-content').val($('#instruction-content').val() + '\t');
	});
	$('#return-btn').click(function(oEvent)
	{
		oEvent.preventDefault();
		$('#instruction-content').val($('#instruction-content').val() + '\n');
	});
	
	$('#jurisdiction-save').click(function(oEvent) 
	{
		oEvent.preventDefault();
		$.ajax(
		{
			'url': 'api/create/0',
			'method': 'POST',
			'data': $('#jurisdiction-form').serialize()
		}).done(function() 
		{
			g_bUpdateJurisdiction = true;
			$('#type-accordion').accordion('option', 'active', false);
			alert("Jurisdiction created successfully");
		}).fail(function()
		{
			alert("Failed to create block");
		});
	});
	$('#title-save').click(function(oEvent) 
	{
		oEvent.preventDefault();
		$.ajax(
		{
			'url': 'api/create/1',
			'method': 'POST',
			'data': $('#title-form').serialize()
		}).done(function() 
		{
			g_bUpdateTitle = true;
			$('#type-accordion').accordion('option', 'active', false);
			alert("Title created successfully");
		}).fail(function()
		{
			alert("Failed to create block");
		});
		console.log($('#title-form').serialize());
	});
	$('#instruction-save').click(function(oEvent) 
	{
		oEvent.preventDefault();
		let oSelect = $('#instruction-situations-to');
		oSelect.children().each(function() {$(this).prop('selected', true);});
		$.ajax(
		{
			'url': 'api/create/2',
			'method': 'POST',
			'data': $('#instruction-form').serialize()
		}).done(function() 
		{
			g_bUpdateInstruction = true;
			$('#type-accordion').accordion('option', 'active', false);
			alert("Instruction created successfully");
		}).fail(function()
		{
			alert("Failed to create block");
		});
		oSelect.children().each(function() {$(this).prop('selected', false);});
		console.log($('#instruction-form').serialize());
	});
	
	$('#situation-save').click(function(oEvent) 
	{
		oEvent.preventDefault();
		$.ajax(
		{
			'url': 'api/create/3',
			'method': 'POST',
			'data': $('#situation-form').serialize()
		}).done(function() 
		{
			g_bUpdateSituation = true;
			$('#type-accordion').accordion('option', 'active', false);
			alert("Block created successfully");
		}).fail(function()
		{
			alert("Failed to create block");
		});
		console.log($('#situation-form').serialize());
	});
	
	$('#tcdtype-save').click(function(oEvent) 
	{
		oEvent.preventDefault();
		let oSelect = $('#tcd-selected-instruction');
		oSelect.children().each(function() {$(this).prop('selected', true);});
		let oJur = $('#tcdtype-jurisdiction');
		$.ajax(
		{
			'url': 'api/create/4',
			'method': 'POST',
			'data': $('#tcdtype-form').serialize()
		}).done(function() 
		{
			$('#type-accordion').accordion('option', 'active', false);
			alert("Traffic Control Device Type created successfully");
		}).fail(function()
		{
			alert("Failed to create block");
		});
		oSelect.children().each(function() {$(this).prop('selected', false);});
		console.log($('#tcdtype-form').serialize());
	});
	$('#init-form').show();
}


function response(oEvent, oUi)
{
	let nIndex = oUi.content.length;
	let sVal = oEvent.target.value;
	while (nIndex-- > 0)
	{
		if (oUi.content[nIndex].label.substr(0, sVal.length).toUpperCase() != sVal.toUpperCase())
			oUi.content.splice(nIndex, 1);
	}
}


function responseAny(oEvent, oUi)
{
	let nIndex = oUi.content.length;
	let sVal = oEvent.target.value;
	while (nIndex-- > 0)
	{
		if (oUi.content[nIndex].label.toUpperCase().indexOf(sVal.toUpperCase()) < 0)
			oUi.content.splice(nIndex, 1);
	}
}


function activateAcc(oEvent, oUi)
{
	$('.tab').hide();
	let nActive = $('#type-accordion').accordion('option', 'active');
	if (nActive === 0 || nActive > 0)
	{
		let sId = oUi.newPanel.prop('id');
		let sSelector = '#' + sId.substring(0, sId.indexOf('-panel')) + '-form';
		$(sSelector).show();
		$(sSelector)[0].reset();
		if (nActive === 0)
			$('#map-container').hide();
		if (nActive === 2)
		{
			$('#instruction-situations-to option').each(function() {$(this).remove();});
			if (g_bUpdateSituation)
			{
				$.ajax(
				{
					'url': 'api/situations',
					'method': 'GET',
					'dataType': 'json'
				}).done(function(oData, sStatus, oXhr)
				{
					g_bUpdateSituations = false;
					let aSits = [];
					for (let [key, value] of Object.entries(oData))
						aSits.push([key, value]);
					aSits.sort(function(a, b) 
					{
						if (a[1] > b[1])
							return 1;
						if (a[1] < b[1])
							return -1;
						return 0;	
					});
					$('#instruction-situations-from option').each(function() {$(this).remove();});	
					let oSelect = $('#instruction-situations-from');
					for (let aSit of aSits.values())
						oSelect.append(new Option(aSit[1], aSit[0]));
					$('#instruction-situations-from option').each(function() {$(this).on('dblclick', moveToTo)});
				}).fail(function()
				{
					g_bUpdateTitle = true;
					alert('Failed to load situations');
				});
			}
		}
		else if (nActive === 4)
		{
			$('#tcd-instructions').prop('disabled', true);
			$('#tcd-selected-instructions option').each(function() {$(this).remove();});
		}
	}
	else
		$('#init-form').show();
}

function moveToTo()
{
	$(this).off('dblclick', moveToTo);
	$('#instruction-situations-to').append($(this));
	sortSelectOpts('#instruction-situations-to');
	$('#instruction-situations-to option').each(function() {$(this).on('dblclick', moveToFrom); $(this).prop('selected', false);});
}

function moveToFrom()
{
	$(this).off('dblclick', moveToFrom);
	$('#instruction-situations-from').append($(this));
	sortSelectOpts('#instruction-situations-from');
	$('#instruction-situations-from option').each(function() {$(this).on('dblclick', moveToTo); $(this).prop('selected', false);});
}

function sortSelectOpts(sId)
{
	let oSelect = $(sId);
	let oOpts = oSelect.find('option');
	oOpts.sort(function(a, b)
	{
		return $(a).text().toUpperCase() > $(b).text().toUpperCase() ? 1 : -1;
	});
	
	oSelect.html('').append(oOpts);
}

function fromIntDeg(nOrd)
{
	return nOrd / 10000000.0;
}

$(document).ready(initialize);
