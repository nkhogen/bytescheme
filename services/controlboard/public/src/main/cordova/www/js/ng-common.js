var app = angular.module('app', []);
app.directive('parseStyle', function($interpolate) {
	return function(scope, elem) {
		var exp = $interpolate(elem.html()), watchFunc = function() {
			return exp(scope);
		};
		scope.$watch(watchFunc, function(html) {
			elem.html(html);
		});
	};
});

function guid() {
	function s4() {
		return Math.floor((1 + Math.random()) * 0x10000).toString(16)
				.substring(1);
	}
	return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4()
			+ s4() + s4();
};

function getTriggerTimeSec(hr, min) {
	var seconds = Math.round(new Date().getTime() / 1000);
	seconds += (hr * 60 * 60);
	seconds += (min * 60);
	return seconds;
};

function createPostRequest(target, payload) {
	var request = {
		method : 'POST',
		url : target,
		headers : {
			'Content-Type' : 'application/json'
		},
		data : payload
	}
	return request;
};

function createHttpPayload(session_id, object_id, func_name, params) {
	var request = {
		objectId : object_id,
		name : func_name,
		requestId : guid(),
		sessionId : session_id,
		parameters : params
	};
	return request;
};

function getGlobals() {
	var scheme = "https://";
	var domain = "controller.bytescheme.com";
	var home_url = scheme + domain;
	var globals = {
		webclient_id: '91456297737-dhbh8oepbecs7gj9hfbgfh4896ri1die.apps.googleusercontent.com',
		login_page : home_url,
		login_url : home_url + '/rpc/login',
		logout_url : home_url + '/rpc/logout',
		rpc_url : home_url + "/rpc",
		success_redirect :  "controlboard.html",
		logout_redirect : "index.html",
		poweron_css_class : 'power_on',
		poweroff_css_class : 'power_off',
		func_interval : 20000,
		root_object_id : "00000000-0000-0000-0000-000000000000"
	}
	return globals;
};

function extractOrigin(url) {
	var start = url.indexOf("://");
	var end = url.indexOf("/", start + 3);
	return "https" + url.substring(start, end);
};

function redirectOnLogin() {
	var globals = getGlobals();
	document.location.replace(globals.success_redirect);
};

function redirectOnLogout() {
	var globals = getGlobals();
	document.location.replace(globals.logout_redirect);
};


function checkException(e) {
	if (e) {
		var msg = JSON.stringify(e);
		if (msg.includes("Session") || msg.includes("security check")) {
			redirectOnLogout();
			return true;
		}
	}
	return false;
};

function setCookie(cname, cvalue) {
	var mins = 10;
	var d = new Date();
	d.setTime(d.getTime() + (mins * 60 * 1000));
	var expires = "expires=" + d.toUTCString();
	document.cookie = cname + "=" + cvalue + ";" + expires;
};

function getCookie(cname) {
	var name = cname + "=";
	var ca = document.cookie.split(';');
	for (var i = 0; i < ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0) == ' ') {
			c = c.substring(1);
		}
		if (c.indexOf(name) == 0) {
			return c.substring(name.length, c.length);
		}
	}
	return null;
};

function deleteCookie(cname) {
	var d = new Date();
	d.setTime(d.getTime() - (60 * 1000));
	var expires = "expires=" + d.toUTCString();
	document.cookie = cname + "=;" + expires;
};
