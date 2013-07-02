function HistogramListCtrl($scope, $http) {
	$http.get('histogram').success(function(data) {
		$scope.list = data;
	});

}