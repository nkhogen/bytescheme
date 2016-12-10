/**
 * @author Naorem Khogendro Singh
 */
var app = angular.module('app');

app.controller('auth_ctrl', function($scope, $http) {
	var globals = getGlobals();
	$scope.init = function() {
	};
	$scope.rpc_login = function(googleUser) {
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
			$scope.session = response.data.returnValue;
			if (response.data.exception === null) {
				setCookie("session", $scope.session);
				setCookie("user", profile.getName());
				setCookie("email", email);
				console.log($scope.session);
				redirectOnLogin();
			}
		});
	};
});
app.controller('controlboard_ctrl', function($scope, $http, $timeout, $window) {
	var globals = getGlobals();
	$scope.init = function() {
		$scope.power_state = 0;
		$scope.power_img = globals.poweroff_img;
		$scope.session = getCookie("session");
		if ($scope.session === null) {
			redirectOnLogout();
		}
		$scope.user = getCookie("user");
		$scope.email = getCookie("email");
		var payload = createHttpPayload($scope.session, globals.root_object_id,
				"getControlBoard", [$scope.email]);
		var request = createPostRequest(globals.rpc_url, payload);
		console.log('Calling getControlBoard');
		$http(request).then(function(response) {
			console.log('getControlBoard: '+JSON.stringify(response.data.exception ));
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

	$scope.rpc_logout = function() {
		var request = createPostRequest(globals.logout_url, {
			sessionId : $scope.session,
			requestId : guid()
		});
		$http(request).then(function(response) {
			console.log(response.data);
			$scope.session = null;
			deleteCookie("session");
			deleteCookie("user");
			deleteCookie("email");
			deleteCookie("video");
			redirectOnLogout();
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
				$scope.videos = [];
				for (var device in devices) {
					$scope.devices.push($scope.displayDevice(devices[device]["::value"]));
				}
				$scope.videos.push("Start Video");
			}
			return response.data.exception;
		});
	};
	// Function to replicate setInterval using $timeout service
	// (5s).
	$scope.intervalFunction = function() {
		$timeout(function() {
			var e = $scope.periodicFunction();
			if (!checkException(e)) {
				$scope.intervalFunction();
			}
		}, globals.func_interval)
	};
	$scope.displayDevice = function(device) {
		var extendedDevice = null;
		if (device.powerOn) {
			extendedDevice = {pin : device.pin, tag : device.tag, powerOn : device.powerOn, button_css_class: globals.poweron_css_class};
		} else {
			extendedDevice = {pin : device.pin, tag : device.tag, powerOn : device.powerOn, button_css_class: globals.poweroff_css_class};
		}
		return extendedDevice;
	};

	$scope.startVideo = function() {
		var payload = createHttpPayload($scope.session, $scope.control_object_id, "getVideoUrl", null);
		var request = createPostRequest(globals.rpc_url, payload);
		$http(request).then(function(response) {
			var videoUrl = response.data.returnValue;
			console.log("Response for getVideoUrl: " + videoUrl);
			if (videoUrl == null) {
				return;
			}
			setCookie("video", videoUrl);
			$window.open('video.html', '_blank', 'fullscreen=yes,toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=no,resizable=no');
		});
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
				if (checkException(response.data.exception)) {
					redirectOnLogout();
				}
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