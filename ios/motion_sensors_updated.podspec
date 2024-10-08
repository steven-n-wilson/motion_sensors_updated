#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint motion_sensors_updated.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'motion_sensors_updated'
  s.version          = '0.0.1'  # You must include a version number, even for development
  s.summary          = 'Flutter plugin for accessing the Android and iOS accelerometer, gyroscope, magnetometer, and orientation sensors.'
  
  # Provide a more detailed description
  s.description      = <<-DESC
                       This Flutter plugin allows access to sensors such as accelerometers, gyroscopes, magnetometers, and orientation sensors on Android and iOS devices. 
                       It is useful for applications requiring movement analysis, VR/AR, and other motion-based input features.
                       DESC
  
  s.homepage         = 'https://github.com/steven-n-wilson/motion_sensors_updated'
  
  # Specify a license
  s.license          = { :type => 'MIT', :file => '../LICENSE' }

  s.author           = { 'Your Company' => 'email@example.com' }

  # Use the GitHub repo, and specify a branch if you don’t want to use tags
  s.source           = { :git => 'https://github.com/steven-n-wilson/motion_sensors_updated.git', :branch => 'master' }
  
  s.source_files     = 'Classes/**/*'
  s.dependency       'Flutter'
  s.platform         = :ios, '11.0'

  # Exclude i386 architecture for Flutter
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  
  s.swift_version    = '5.0'
end


