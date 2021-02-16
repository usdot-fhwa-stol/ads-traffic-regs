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
	$.ajax('api/jurisdictions',
	{
		dataType: 'json',
		method: 'GET'
	}).done(jurisdictionSuccess);
}

function jurisdictionSuccess(oData, sStatus, oJqXHR)
{
	let aNames = [];
	for (let [id, name] of Object.entries(oData))
		aNames.push([name, id]);
	
	aNames.sort((a, b) => strCmp(a[0], b[0]));
	
	let oJurList = $('#jurList');
	for (let aEntries of aNames.values())
	{
		let sItem = '<li id="' + aEntries[1] + '">';
		sItem += aEntries[0];
		sItem += '</li>';
		oJurList.append(sItem);
		
		$('#' + aEntries[1]).click(function() 
			{
				if ($(this).hasClass('w3-sw-kale-green'))
				{
					$(this).removeClass('w3-sw-kale-green');
					$('#instructList').empty();
				}
				else
				{
					$('#jurList li').removeClass('w3-sw-kale-green');
					$.ajax('api/instructions', {dataType:'json', method: 'POST', data: {'id': aEntries[1]}}).done(instructionSuccess);
					$(this).addClass('w3-sw-kale-green');
				}
			});
	}
}


function instructionSuccess(oData, sStatus, oJQXHR)
{
	let oInstructList = $('#instructList');
	oInstructList.empty();
	
	let oTitles = Object.values(oData);
	oTitles.sort((a, b) => titleCmp(a, b));
	for (let oTitle of oTitles.values())
	{
		let sTitle = oTitle.titlename;
		let oSituations = oTitle.situations;
		oTitle.instructions.sort((a, b,) => instructCmp(a, b));
		
		for (let oInstruction of oTitle.instructions.values())
		{
			let sItem = '<li class="hoverable-li" id="' + oInstruction.currid + '">';
			let sTitleBar = sTitle + '<br>' + oInstruction.label + '<br>';
			for (let sSituationId of oInstruction.situations.values())
			{
				sTitleBar += oSituations[sSituationId] + ', ';
			}
			sTitleBar = sTitleBar.slice(0, sTitleBar.length - 2);
			sItem += sTitleBar;
			sItem +='</li>';
			oInstructList.append(sItem);

			$('#' + oInstruction.currid).click(function() 
				{
					$('#instructList li').removeClass('w3-sw-kale-green');
					$(this).addClass('w3-sw-kale-green');
					let oDialog = $('#instructDialog');
					oDialog.parent().attr('tabindex', 0);
					oDialog.siblings().find(".ui-dialog-title").html(sTitleBar);
					oDialog.html('<pre style="white-space: pre-wrap; overflow-y: auto;">\n' + oInstruction.content + '\n</pre>');
					oDialog.dialog('open');
				});
		}
	}
}


function createDialog(event, ui)
{
	$(this).parent().css({'max-width': 'calc(80% - 50px', 'max-height': 'calc(80% - 50px'});
}


function openDialog(event, ui)
{
	$(this).css('max-height', parseFloat($(this).parent().css('height')) * 0.9);
//	$("body").prepend("<div id='PopupMask' style='position:fixed;width:100%;height:100%;z-index:10;background-color:gray;'></div>");
//	$("#PopupMask").css('opacity', 0.8);
//	$(this).data('saveZindex', $(this).css('z-index'));
////	$(this).data('savePosition', $(this).css('position'));
//	$(this).css('z-index', '11');
}

function afterResize(event, ui)
{
	$(this).css('max-height', parseFloat($(this).parent().css('height')) * 0.9);
}

function closeDialog(event, ui)
{
	$('#instructList li').removeClass('w3-sw-kale-green');
//	if ($('#PopupMask') == null)
//		return;
//	$('#PopupMask').remove();
//	$(this).css('z-index', $(this).data('saveZindex'));
}


function strCmp(s1, s2)
{
	let len1 = s1.length;
	let len2 = s2.length;
	let lim = Math.min(len1, len2);

	let k = 0;
	while (k < lim) 
	{
		let c1 = s1[k];
		let c2 = s2[k++];
		if (c1 != c2)
			return c1.charCodeAt(0) - c2.charCodeAt(0);
	}
	return len1 - len2;
}


function titleCmp(o1, o2)
{
	return strCmp(o1.titlename, o2.titlename);
}


function instructCmp(o1, o2)
{
	return strCmp(o1.label, o2.label);
}


$(document).ready(initialize);
