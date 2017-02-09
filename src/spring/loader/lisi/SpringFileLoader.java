package spring.loader.lisi;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class SpringFileLoader {

	public static boolean loadJarFile(String jarFileUrl, String packageName, XmlWebApplicationContext context) {
		try {
			//����޷��ɹ�����jar�����˳�
			if (!loadToClassLoader(jarFileUrl)) {
				return false;
			}
			// ע��ע�ⷽʽ��bean
			//��ȡwebӦ����ʹ�õ�bean������jar���е�bean����ע�뵽������
			DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
			//����Spring��ע���Զ�ɨ����(���캯������������ɨ����beanע�������)��������ȡ����ע���bean��ɨ������Ҫָ��Ҫɨ��İ�����ɨ��jar�еİ�·����ʹ��ͨ�����
			//�����ڵ���jarʱ����ѡ��add directory entries������Ŀ¼Ҳ���뵽jar�У�������springɨ��ʱ���޷��ҵ�class
			ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(defaultListableBeanFactory);
			//��ʼɨ��
			scanner.scan(packageName);
			
			// ע��xml�����е�bean����������
			String configurationFilePath = "jar:file://" + jarFileUrl + "!/resources/applicationContext.xml";
			URL url = new URL(configurationFilePath);
			UrlResource urlResource = new UrlResource(url);
			//����һ��DefaultListableBeanFactory���������ڻ�ȡapplicationContext.xml�ļ������õ�bean
			DefaultListableBeanFactory xmlBeanFactory = new DefaultListableBeanFactory();
			//�ø�DefaultListableBeanFactory��������һ��XmlBeanDefinitionReader�����ڼ���bean����
			XmlBeanDefinitionReader xmlBeanReader = new XmlBeanDefinitionReader(xmlBeanFactory);
			//����applicationContext.xml�ļ������õ�bean
			xmlBeanReader.loadBeanDefinitions(urlResource);
			//��ȡ������������bean������
			String[] beanIds = xmlBeanFactory.getBeanDefinitionNames();
			//������Щ�µ�bean�����뵽defaultListableBeanFactoryӦ��������
			for (String beanId : beanIds) {
				BeanDefinition bd = xmlBeanFactory.getMergedBeanDefinition(beanId);
				defaultListableBeanFactory.registerBeanDefinition(beanId, bd);
			}
			
			//��ȡSpringMVC�е�HandlerMapping����������Ҳ��һ��bean��HandlerMapping���������ӳ��ΪHandlerExecutionChain����
			//������һ��Handler��������ҳ������������󡢶��HandlerInterceptor������������
			ManualAnnotationHandlerMapping handlerMapping;
			try {
				handlerMapping = context.getBean(ManualAnnotationHandlerMapping.class);
				//���handlerMapping��Ϊ�գ�������SpringMVC��Ŀ�������¼�jar���е�ӳ���ϵ��handlerMapping��
				if (null != handlerMapping) {
					String[] beansName = defaultListableBeanFactory.getBeanDefinitionNames();
					//��������������bean
					for (String beanName : beansName) {
						//��ȡbean��Ӧ��ӳ��url
						String[] mappedURLs = handlerMapping.determineUrlsForHandlerByManual(beanName);
						//ӳ��url��Ϊ�գ�������bean��controller���е���Ӧ�������Ѹ�bean�Ͷ�Ӧӳ��url�Ĺ�ϵд��handlerMapping
						if (!ObjectUtils.isEmpty(mappedURLs)) {
							handlerMapping.registerHandlerByManual(mappedURLs, beanName);
						}
					}
				}
			} catch (Exception e) {
				//�޷���ȡManualAnnotationHandlerMapping,��������Ŀ����springmvc
			}
		
//			System.out.println(context.getBean("collectController"));

			return true;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return false;
	}

	/**
	 * ��jar�����ص��������
	 * @param jarFileUrl
	 * @throws MalformedURLException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private static boolean loadToClassLoader(String jarFileUrl) throws MalformedURLException, NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		File jarFile = new File(jarFileUrl);
		URL jarUrl = jarFile.toURI().toURL();
		// ����jar�ļ����������Ϊһ�����class�ļ����ļ���
		Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		boolean accessible = method.isAccessible(); // ��ȡ�����ķ���Ȩ��
		try {
			if (accessible == false) {
				method.setAccessible(true); // ���÷����ķ���Ȩ��
			}
			// ��ȡ��ǰ�߳��������
			URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
			method.invoke(classLoader, jarUrl);
			return true;
		} catch (Exception ex) {
			return false;
		} finally {
			method.setAccessible(accessible);
		}
	}
}
