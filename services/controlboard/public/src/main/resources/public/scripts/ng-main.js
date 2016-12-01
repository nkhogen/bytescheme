/**
 * @author Naorem Khogendro Singh
 */
var app = angular.module('app');

app.controller('auth_ctrl', function($scope, $http) {
	var globals = getGlobals();
	$scope.init = function() {
	};
	$scope.onSignIn = function(googleUser) {
		var profile = googleUser.getBasicProfile();
		var auth = googleUser.getAuthResponse();
		var user = profile.getName();
		var email = profile.getEmail();
		console.log('Name: ' + user);
		console.log('Email: ' + email);
		var id_token = auth["id_token"];
		var request = createPostRequest(globals.login_url, {
			user : email,
			password : id_token,
			requestId : guid()
		});
		$http(request).then(function(response) {
			console.log(response.data);
			$scope.session_id = response.data.returnValue;
			if (response.data.exception === null) {
				window.sessionStorage.setItem("session", $scope.session_id);
				window.sessionStorage.setItem("user", profile.getName());
				window.sessionStorage.setItem("email", email);
				console.log($scope.session_id);
				redirectOnLogin();
			}
		});
	};
	window.onSignIn = $scope.onSignIn;
});
app.controller('controlboard_ctrl', function($scope, $http, $timeout) {
	var globals = getGlobals();
	$scope.init = function() {
		$scope.power_state = 0;
		$scope.power_img = globals.poweroff_img;
		$scope.session = window.sessionStorage.getItem("session");
		if ($scope.session === null) {
			redirectOnLogout();
		}
		$scope.user = window.sessionStorage.getItem("user");
		$scope.email = window.sessionStorage.getItem("email");
		var payload = createHttpPayload($scope.session, globals.root_object_id,
				"getControlBoard", [$scope.email]);
		var request = createPostRequest(globals.rpc_url, payload);
		console.log('Calling getControlBoard');
		$http(request).then(function(response) {
			console.log('getControlBoard: '+response.data.returnValue);
			if (response.data.returnValue) {
				$scope.control_object_id = JSON.parse(response.data.returnValue)["::objId"];
				if ($scope.control_object_id) {
					$scope.periodicFunction();
					$scope.intervalFunction();
					return;
				}
			}
		});
	};
	$scope.periodicFunction = function() {
		var payload = createHttpPayload($scope.session,
				$scope.control_object_id, "listDevices", null);
		var request = createPostRequest(globals.rpc_url, payload)
		$http(request).then(function(response) {
			console.log('listDevices: '+response.data.returnValue);
			if (response.data.returnValue) {
				var devices = JSON.parse(response.data.returnValue);
				$scope.devices = [];
				for (var device in devices) {
					$scope.devices.push($scope.displayDevice(devices[device]["::value"]));
				}
			}
		});
	};
	// Function to replicate setInterval using $timeout service
	// (5s).
	$scope.intervalFunction = function() {
		$timeout(function() {
			$scope.periodicFunction();
			$scope.intervalFunction();
		}, globals.func_interval)
	};

	$scope.signOut = function() {
		var auth2 = gapi.auth2.getAuthInstance();
		auth2.signOut().then(function() {
			var request = createPostRequest(globals.logout_url, {
				sessionId : $scope.session,
				requestId : guid()
			});
			$http(request).then(function(response) {
				console.log(response.data);
			});
			$scope.session_id = null;
			window.sessionStorage.clear();
			redirectOnLogout();
			console.log('User signed out.');
		});
	}
	window.signOut = $scope.signOut;
	$scope.displayDevice = function(device) {
		var extendedDevice = null;
		if (device.powerOn) {
			extendedDevice = {pin : device.pin, tag : device.tag, powerOn : device.powerOn, button_css_class: globals.poweron_css_class};
		} else {
			extendedDevice = {pin : device.pin, tag : device.tag, powerOn : device.powerOn, button_css_class: globals.poweroff_css_class};
		}
		return extendedDevice;
	};
	/* Click handler starts */
	$scope.clickHandler = function(device) {
		var next_device = {
			pin : device.pin,
			tag : device.tag,
			powerOn : !(device.powerOn)
		};
		var payload = createHttpPayload($scope.session,
				$scope.control_object_id, "changePowerStatus", [JSON.stringify(next_device)]);
		var request = createPostRequest(globals.rpc_url, payload);
		$http(request).then(function(response) {
			console.log("Response for changePowerStatus: " + response.data.returnValue);
			if (response.data.returnValue == null) {
				return;
			}
			var device = JSON.parse(response.data.returnValue);
			var extendedDevice = $scope.displayDevice(device);
			for(var d in $scope.devices) {
				if($scope.devices[d].pin == extendedDevice.pin) {
					$scope.devices[d].powerOn = extendedDevice.powerOn;
					$scope.devices[d].button_css_class = extendedDevice.button_css_class;
					return;
				}
			}
			console.log('New device added');
			$scope.devices.push(extendedDevice);
		});
	};
});
