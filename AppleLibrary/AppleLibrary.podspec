Pod::Spec.new do |s|
  s.name             = 'AppleLibrary'
  s.version          = '0.1.0'
  s.summary          = 'A short description of AppleLibrary.'
  s.description      = 'Add long description of the pod here'
  s.homepage         = 'https://github.com/Kotlin/kotlin-with-cocoapods-library-sample'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'andrey.yastrebov' => 'andrey.yastrebov@jetbrains.com' }
  s.source           = { :git => 'https://github.com/Kotlin/kotlin-with-cocoapods-library-sample.git', :tag => s.version.to_s }

  s.ios.deployment_target = '16.0'
  s.swift_version = '6.0'
  s.source_files = 'AppleLibrary/Classes/**/*'
  s.dependency 'SwiftyJSON', '~> 4.0'
end
