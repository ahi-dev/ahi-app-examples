//
//  AHI - Example Code
//
//  Copyright (c) Advanced Human Imaging. All rights reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

import SwiftUI

public struct AHIConfigTokens {
    /// Your AHI MultiScan DEV token
    static let AHI_MULTI_SCAN_TOKEN = ""
    /// Your user id. Hardcode a valid user id for testing purposes.
    static let AHI_TEST_USER_ID = "AHI_TEST_USER"
    /// Your salt token.
    static let AHI_TEST_USER_SALT = "user"
    /// Any claims you require passed to the SDK.
    static let AHI_TEST_USER_CLAIMS = ["test"]
}

struct ContentView: View {
    var body: some View {
        VStack() {
            Button (action:{
                }, label: {
                    Text("Setup SDK")
                        .foregroundColor(Color.white)
                        .frame(maxWidth: .infinity)
                        })
            .frame(height: 55.0)
            .background(Color.black)
            Spacer()
        }
        .padding(EdgeInsets(top: 71, leading: 16, bottom: 16, trailing: 16))
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
