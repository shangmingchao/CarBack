package cn.frank.carback.page

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

/**
 * Fragment 扩展
 *
 * @author shangmingchao
 */

/**
 * 基于反射的 BaseFragment 扩展
 */
interface FragmentBinding<VB : ViewBinding> {
    val binding: VB
    fun Fragment.createViewWithBinding(inflater: LayoutInflater, container: ViewGroup?): View
}

class FragmentBindingDelegate<VB : ViewBinding> : FragmentBinding<VB> {
    private var _binding: VB? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override val binding: VB
        get() = requireNotNull(_binding) { "$this ViewBinding has been destroyed." }

    override fun Fragment.createViewWithBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View {
        if (_binding == null) {
            _binding = inflateWithGeneric(this, inflater, container, false)
            viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    handler.post { _binding = null }
                }
            })
        }
        return _binding!!.root
    }
}


fun <VB : ViewBinding> inflateWithGeneric(
    genericOwner: Any,
    layoutInflater: LayoutInflater,
    parent: ViewGroup?,
    attachToParent: Boolean
): VB =
    withGenericBindingClass<VB>(genericOwner) { clazz ->
        clazz.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
            .invoke(null, layoutInflater, parent, attachToParent) as VB
    }


private fun <VB : ViewBinding> withGenericBindingClass(
    genericOwner: Any,
    block: (Class<VB>) -> VB
): VB {
    var genericSuperclass = genericOwner.javaClass.genericSuperclass
    var superclass = genericOwner.javaClass.superclass
    while (superclass != null) {
        if (genericSuperclass is ParameterizedType) {
            genericSuperclass.actualTypeArguments.forEach {
                try {
                    return block.invoke(it as Class<VB>)
                } catch (e: NoSuchMethodException) {
                } catch (e: ClassCastException) {
                } catch (e: InvocationTargetException) {
                    var tagException: Throwable? = e
                    while (tagException is InvocationTargetException) {
                        tagException = e.cause
                    }
                    throw tagException
                        ?: IllegalArgumentException("ViewBinding generic was found, but creation failed.")
                }
            }
        }
        genericSuperclass = superclass.genericSuperclass
        superclass = superclass.superclass
    }
    throw IllegalArgumentException("There is no generic of ViewBinding.")
}
