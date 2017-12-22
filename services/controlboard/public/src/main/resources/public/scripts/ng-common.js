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
		client_id : "91456297737-d1p2ha4n2847bpsrdrcp72uhp614ar9q.apps.googleusercontent.com",
		login_page : home_url,
		login_url : home_url + '/rpc/login',
		logout_url : home_url + '/rpc/logout',
		rpc_url : home_url + "/rpc",
		success_redirect : home_url + "/controlboard.html",
		logout_redirect : home_url,
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
	var url = "https://accounts.google.com/o/oauth2/v2/auth?scope=email&client_id="
			+ globals.client_id
			+ "&redirect_uri="
			+ globals.success_redirect
			+ "&response_type=token";
	document.location.replace(url);
};

function redirectOnLogout() {
	var globals = getGlobals();
	window.location
			.replace("https://www.google.com/accounts/Logout?continue=https://appengine.google.com/_ah/logout?continue="
					+ globals.logout_redirect);
};

function doInScope(appName, callback) {
	var appElement = document.querySelector('[ng-app=' + appName + ']');
	var $scope = angular.element(appElement).scope();
	$scope.$apply(callback($scope));
};

function onSignIn(googleUser) {
	doInScope('app', function(scope) {
		scope.rpc_login(googleUser);
	});
};

function signOut() {
	var auth2 = gapi.auth2.getAuthInstance();
	auth2.signOut().then(function() {
		doInScope('app', function(scope) {
			scope.rpc_logout();
		});
	});
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
	document.cookie = cname + "=" + cvalue + ";" + expires
			+ ";domain=bytescheme.com";
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