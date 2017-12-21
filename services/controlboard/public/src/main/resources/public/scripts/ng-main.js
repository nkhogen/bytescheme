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
			$scope.session = response.data.returnValue;
			if (response.data.exception === null) {
				console.log("Logged in succesfully");
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
				"getControlBoard", null);
		var request = createPostRequest(globals.rpc_url, payload);
		console.log('Calling getControlBoard');
		$http(request).then(function(response) {
			console.log('getControlBoard: '+JSON.stringify(response.data.exception));
			console.log('getControlBoard: '+response.data.returnValue);
			if (response.data.returnValue == null) {
				return;
			}
			$scope.control_object_id = JSON.parse(response.data.returnValue)["::objId"];
			payload = createHttpPayload($scope.session, globals.root_object_id,
					"getDeviceEventScheduler", null);
			request = createPostRequest(globals.rpc_url, payload);
			console.log('Calling getDeviceEventScheduler');
			$http(request).then(function(response) {
				console.log('getDeviceEventScheduler: '+JSON.stringify(response.data.exception));
				console.log('getDeviceEventScheduler: '+response.data.returnValue);
				if (response.data.returnValue == null) {
					return;
				}
				$scope.scheduler_object_id = JSON.parse(response.data.returnValue)["::objId"];
				$scope.periodicFunction();
				$scope.intervalFunction();
				$scope.initDisplay();
			});
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
		$scope.listDevices(function() {
			console.log("Done fetching devices");
			$scope.listEvents(function(){
				console.log("Done fetching events");
			});
		});
	};

	$scope.listDevices = function(callback) {
		var payload = createHttpPayload($scope.session,
				$scope.control_object_id, "listDevices", null);
		var request = createPostRequest(globals.rpc_url, payload)
		$http(request).then(function(response) {
			console.log('listDevices: '+response.data.returnValue);
			if (response.data.returnValue == null) {
				return;
			}
			var values = JSON.parse(response.data.returnValue);
			$scope.devices = {};
			$scope.videos = {v1 : "Start Video"};
			for (var key in values) {
				var device = values[key]["::value"];
				$scope.devices[device.deviceId] = $scope.displayDevice(device);
			}
			callback();
		});
	};

	$scope.listEvents = function(callback) {
		var payload = createHttpPayload($scope.session,
				$scope.scheduler_object_id, "list", null);
		var request = createPostRequest(globals.rpc_url, payload)
		$http(request).then(function(response) {
			console.log('list: '+response.data.returnValue);
			if (response.data.returnValue == null) {
				return;
			}
			var values = JSON.parse(response.data.returnValue);
			$scope.events = {};
			for (var key in values) {
				var event = values[key]["::value"];
				var extendedEvent = $scope.extendEvent(event);
				if (extendedEvent) {
					$scope.events[event.deviceId] = extendedEvent;
				}
			}
			callback();
		});
	};

	$scope.isAnyDeviceAvailable = function(){
		return Object.keys($scope.devices).length > 0;
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

	$scope.initDisplay = function() {
		$scope.hours = [];
	    for (h = 0; h < 24; h++) {
	       $scope.hours.push(h);
	    }
	    $scope.mins = [];
	    for (m = 0; m < 60; m++) {
	       $scope.mins.push(m);
	    }
	    $scope.statuss = ["ON", "OFF"];
	};

	$scope.displayDevice = function(device) {
		var extendedDevice = null;
		if (device.powerOn) {
			extendedDevice = {deviceId : device.deviceId, tag : device.tag, powerOn : device.powerOn, button_css_class: globals.poweron_css_class};
		} else {
			extendedDevice = {deviceId : device.deviceId, tag : device.tag, powerOn : device.powerOn, button_css_class: globals.poweroff_css_class};
		}
		return extendedDevice;
	};

	$scope.extendEvent = function(event) {
		var extendedEvent = null;
		var dId = event.deviceId.toString();
		if (dId in $scope.devices) {
			var device = $scope.devices[dId];
			var date = new Date(0);
			date.setUTCSeconds(event.triggerTime);
			var powerStatus = "OFF";
			if (event.powerOn) {
				powerStatus = "ON";
			}
			extendedEvent = {id: event.id, schedulerId: event.schedulerId, deviceId : event.deviceId, tag: device.tag, triggerTime : date.toTimeString(), powerOn : powerStatus};
		}
		return extendedEvent;
	};

	$scope.startVideo = function(videoId) {
		var payload = createHttpPayload($scope.session, $scope.control_object_id, "getVideoUrl", null);
		var request = createPostRequest(globals.rpc_url, payload);
		$http(request).then(function(response) {
			var videoUrl = response.data.returnValue;
			console.log("Response for getVideoUrl: " + videoUrl);
			if (videoUrl == null) {
				if (checkException(response.data.exception)) {
					redirectOnLogout();
				}
				return;
			}
			setCookie("video", videoUrl);
			$window.location.href='video.html';
			// $window.open('video.html', '_blank',
			// 'fullscreen=yes,toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=no,resizable=no');
		});
	};

	/* Power click handler starts */
	$scope.powerHandler = function(device) {
		var next_device = {
			deviceId : device.deviceId,
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
			$scope.devices[device.deviceId] = $scope.displayDevice(device);
		});
	};

	$scope.scheduleEventHandler = function(event) {
		var eventDetails = {
				deviceId : event.device.deviceId,
				triggerTime: getTriggerTimeSec(event.hour, event.min),
				powerOn: (event.status == 'ON')
		};
		var payload = createHttpPayload($scope.session,
				$scope.scheduler_object_id, "schedule", [JSON.stringify(eventDetails)]);
		var request = createPostRequest(globals.rpc_url, payload);
		$http(request).then(function(response) {
			console.log("Response for schedule: " + response.data.returnValue);
			if (response.data.returnValue == null) {
				if (checkException(response.data.exception)) {
					redirectOnLogout();
				}
				return;
			}
			$scope.periodicFunction();
		});
	};

	$scope.cancelEventHandler = function(event) {
		var eventDetails = {
				id : event.id,
				schedulerId : event.schedulerId,
				user : $scope.email
		};
		var payload = createHttpPayload($scope.session,
				$scope.scheduler_object_id, "cancel", [JSON.stringify(eventDetails)]);
		var request = createPostRequest(globals.rpc_url, payload);
		$http(request).then(function(response) {
			console.log("Response for delete: " + response.data.returnValue);
			if (response.data.returnValue == null) {
				if (checkException(response.data.exception)) {
					redirectOnLogout();
				}
				return;
			}
			$scope.periodicFunction();
		});
	};
});
