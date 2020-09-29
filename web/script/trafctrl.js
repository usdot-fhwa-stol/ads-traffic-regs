function initialize()
{
	$('#controlDialog').dialog(
					{width: 400,
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
					$('#controlList').empty();
				}
				else
				{
					$('#jurList li').removeClass('w3-sw-kale-green');
					$.ajax('api/tcdtypes', {dataType:'json', method: 'POST', data: {'id': aEntries[1]}}).done(tcdtypeSuccess);
					$(this).addClass('w3-sw-kale-green');
				}
			});
	}
}


function tcdtypeSuccess(oData, sStatus, oJQXHR)
{
	let oControlList = $('#controlList');
	oControlList.empty();

	oData.sort((a, b) => controlCmp(a, b));
	for (let oTcdType of oData.values())
	{
		let sItem = '<li class="hoverable-li" id="' + oTcdType.currid + '">';
		let sTitleBar = oTcdType.label + ' ' + oTcdType.descr;
		
		
//		let sSvg = oTcdType.svg;
//		let nStart = sSvg.indexOf('viewBox=') + 'viewBox='.length + 1;
//		nStart = sSvg.indexOf(' ', nStart) + 1;
//		nStart = sSvg.indexOf(' ', nStart) + 1;
//		let nEnd = sSvg.indexOf(' ', nStart);
//		sTitleBar += '<br>' + sSvg.substring(nStart, nEnd) + ' x ';
//		nStart = nEnd + 1;
//		nEnd = sSvg.indexOf('\'', nStart);
//		sTitleBar += sSvg.substring(nStart, nEnd) + ' ' + oTcdType.units;
		
		if (oTcdType.instrs.length > 0)
		{
			sTitleBar += '<br>';
			for (let sInstr of oTcdType.instrs.values())
				sTitleBar += sInstr + ', ';
			
			sTitleBar = sTitleBar.substring(0, sTitleBar.length - 2);
		}
		sItem += sTitleBar;
		sItem +='</li>';
		oControlList.append(sItem);
		$('#' + oTcdType.currid).click(function()
			{
				$('#controlList li').removeClass('w3-sw-kale-green');
				$(this).addClass('w3-sw-kale-green');
				let oDialog = $('#controlDialog');

				oDialog.siblings().find(".ui-dialog-title").html(sTitleBar);
				oDialog.html(oTcdType.svg);
				oDialog.dialog('open');
			});
	}
}


function createDialog(event, ui)
{
//	$(this).parent().css({'max-height': 'calc(90% - 50px'});
}


function openDialog(event, ui)
{
//	let nTitleHeight = $(this).siblings().find(".ui-dialog-title").height();
//	$(this).css({'max-height': parseFloat($(this).parent().css('height'))* 0.9 - nTitleHeight + 'px'});
//	$("body").prepend("<div id='PopupMask' style='position:fixed;width:100%;height:100%;z-index:10;background-color:gray;'></div>");
//	$("#PopupMask").css('opacity', 0.8);
//	$(this).data('saveZindex', $(this).css('z-index'));
//	$(this).data('savePosition', $(this).css('position'));
//	$(this).css('z-index', '11');
}

function afterResize(event, ui)
{
//	$(this).css('max-height', parseFloat($(this).parent().css('height')) * 0.9);
}

function closeDialog(event, ui)
{
	$('#controlList li').removeClass('w3-sw-kale-green');
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


function controlCmp(o1, o2)
{
	return strCmp(o1.label, o2.label);
}


$(document).ready(initialize);