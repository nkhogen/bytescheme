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
	var base_url = 'https://controller.bytescheme.com/rpc';
	var globals = {
		login_page : "https://controller.bytescheme.com",
		controlboard_page : "https://controller.bytescheme.com/controlboard.html",
		login_url : base_url + '/login',
		logout_url : base_url + '/logout',
		rpc_url : base_url,
		poweron_css_class : 'power_on',
		poweroff_css_class : 'power_off',
		func_interval : 30000,
		root_object_id : "00000000-0000-0000-0000-000000000000"
	}
	return globals;
};

function redirectOnLogin() {
	var globals = getGlobals();
	var url = "https://accounts.google.com/o/oauth2/v2/auth?scope=email&client_id=91456297737-d1p2ha4n2847bpsrdrcp72uhp614ar9q.apps.googleusercontent.com&redirect_uri="
			+ encodeURIComponent(globals.controlboard_page)
			+ "&response_type=token";
	window.location.replace(url);
};

function redirectOnLogout() {
	var globals = getGlobals();
	document.location.href = "https://www.google.com/accounts/Logout?continue=https://appengine.google.com/_ah/logout?continue="
			+ encodeURIComponent(globals.login_page);
};
