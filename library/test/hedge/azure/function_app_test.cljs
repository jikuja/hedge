  (ns hedge.azure.function-app-test
  (:require [cljs.test :refer-macros [deftest is testing async]]
            [cljs.core.async :refer [chan put!]]
            [goog.object :as gobj]
            [hedge.azure.function-app :refer [azure-function-wrapper] :refer-macros [azure-function]]))

(enable-console-print!)

(deftest azure-function-wrapper-test-calls-done-after
  (testing "should call context done with the result given by the handler"
      (async done
        ((azure-function-wrapper (constantly "result")) 
          #js {:done #(do (is (= "result" %2)) (done))}))))
          ;#js {:done #(do (is (= "async-result" %2)) (done))}))))
        
(deftest azure-function-wrapper-test-serialization
  (testing "should serialize the object returned by handler to camel case js object"
      (async done
             ((azure-function-wrapper (constantly {:test-data "Data"}))
              #js {:done #(do
                            (is (= (.-testData %2) 
                                   "Data"))
                            (done))}))))

(deftest azure-function-wrapper-test-headers
    (testing "should not camel case header fields"
      (async done
             ((azure-function-wrapper (constantly {:headers {:Content-Type "application/json+transit"}}))
              #js {:done #(do
                            (is (= (gobj/getValueByKeys %2 "headers" "Content-Type")
                                   "application/json+transit"))
                            (done))}))))

(deftest azure-function-wrapper-test-deserialize
    (testing "should deserialize the arguments to maps with dashed keywords"
      (async done
             ((azure-function-wrapper (fn [& args] (is (= [{:done done} {:test-data {:some-field "Data"}}] args))))
              #js {:done done}
              #js {"testData" #js {"someField" "Data"}}))))

(deftest azure-function-wrapper-test-readport
    (testing "handler returning a core async ReadPort"
      (async done
        (let [results (chan)
              azure-fn (azure-function-wrapper (constantly results))]
          (testing "should complete on receival of a message"
            (azure-fn #js {:done #(do (is (= "result" %2)) (done))})
            (put! results "result"))))))


(deftest azure-function-test
  (testing "azure function"

    (testing "should have a cli function that returns nil"
      (azure-function (constantly :result))

      (is (= (*main-cli-fn*)
             nil)))
    (testing "should export the a wrapped handler"
      (async done
        (azure-function (fn [& args] (is (= [{:done done} {:test-data {:some-field "Data"}}] args))))
        ((gobj/get js/module "exports")
         #js {:done done}
              #js {"testData" #js {"someField" "Data"}})))))
