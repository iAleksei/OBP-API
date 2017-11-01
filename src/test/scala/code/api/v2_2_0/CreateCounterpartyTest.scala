package code.api.v2_2_0

import code.api.ResourceDocs1_4_0.SwaggerDefinitionsJSON
import code.api.util.APIUtil.OAuth._
import code.api.util.ErrorMessages
import code.model.{AccountId, ViewId}
import code.setup.DefaultUsers
import net.liftweb.json.JsonAST.{JArray, JField, JObject, JString}
import net.liftweb.json.Serialization.write

class CreateCounterpartyTest extends V220ServerSetup with DefaultUsers {
  
  //The test Body should use the varible in SwaggerDefinitionsJson, 
  // Because this body will be used in API-Explorer 
  val counterpartyPostJSON = SwaggerDefinitionsJSON.postCounterpartyJSON

  override def beforeAll() {
    super.beforeAll()
  }

  override def afterAll() {
    super.afterAll()
  }

  feature("Assuring that endpoint 'Create counterparty for an account' works as expected - v2.1.0") {

    scenario("There is a user has the owner view and the BankAccount") {

      Given("The user ower access and BankAccount")
      val testBank = createBank("transactions-test-bank1")
      val bankId = testBank.bankId
      val accountId = AccountId("__acc1")
      val viewId =ViewId("owner")


      // Note: The view created below has can_add_counterparty set to true
      // TODO Add a test to test the creation of that permission on a view that doesn't have it, and then try to create the Couterparty
      val bankAccount = createAccountAndOwnerView(Some(resourceUser1), bankId, accountId, "EUR")

      When("We make the request Create counterparty for an account")
      val requestPost = (v2_2Request / "banks" / bankId.value / "accounts" / accountId.value / viewId.value / "counterparties" ).POST <@ (user1)
      val responsePost = makePostRequest(requestPost, write(counterpartyPostJSON))

      Then("We should get a 200 and check all the fields")
      responsePost.code should equal(200)

      var accountRoutingAddress = (responsePost.body \ "other_account_routing_address" ) match {
        case JString(i) => i
        case _ => ""
      }
      
      accountRoutingAddress should equal(counterpartyPostJSON.other_account_routing_address)

    }

    scenario("No BankAccount in Database") {
      Given("The user, but no BankAccount")

      val testBank = createBank("transactions-test-bank")
      val bankId = testBank.bankId
      val accountId = AccountId("notExistingAccountId")
      val viewId =ViewId("owner")
      val ownerView = createOwnerView(bankId, accountId)
      grantAccessToView(resourceUser1, ownerView)

      val requestPost = (v2_2Request / "banks" / bankId.value / "accounts" / accountId.value / viewId.value / "counterparties" ).POST <@ (user1)
      val responsePost = makePostRequest(requestPost, write(counterpartyPostJSON))
      Then("We should get a 400")
      responsePost.code should equal(400)

      val error = for { JObject(o) <- responsePost.body; JField("error", JString(error)) <- o } yield error
      error  should contain  (ErrorMessages.AccountNotFound)
    }

    scenario("counterparty is not unique for name/bank_id/account_id/view_id") {
      Given("The user ower access and BankAccount")
      val testBank = createBank("transactions-test-bank")
      val bankId = testBank.bankId
      val accountId = AccountId("__acc1")
      val viewId =ViewId("owner")
      val bankAccount = createAccountAndOwnerView(Some(resourceUser1), bankId, accountId, "EUR")

      When("We make the request Create counterparty for an account")
      val requestPost = (v2_2Request / "banks" / bankId.value / "accounts" / accountId.value / viewId.value / "counterparties" ).POST <@ (user1)
      var responsePost = makePostRequest(requestPost, write(counterpartyPostJSON))

      Then("We make the request again, the same name/bank_id/account_id/view_id")
      responsePost = makePostRequest(requestPost, write(counterpartyPostJSON))

      Then("We should get a 400 and check the error massage")
      responsePost.code should equal(400)

      val error = for { JObject(o) <- responsePost.body; JField("error", JString(error)) <- o } yield error
      error  should contain  (ErrorMessages.CounterpartyAlreadyExists)

    }
  }

}